package trie

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"net"
	"os"
	"sort"
	"strings"

	"github.com/nqmgaming/blockads-tunnel/internal/bloom"
)

// CompileFilterList downloads a filter list from rawPath (local file path),
// parses domains, and writes .trie and .bloom files.
// Returns the number of domains compiled, or an error.
func CompileFilterList(inputPath, triePath, bloomPath string) (int, error) {
	f, err := os.Open(inputPath)
	if err != nil {
		return 0, fmt.Errorf("open input: %w", err)
	}
	defer f.Close()

	// Pass 1: collect unique domains (streaming, line-by-line)
	domains := make(map[string]struct{})
	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 256*1024), 256*1024)
	for scanner.Scan() {
		lineBytes := bytes.TrimSpace(scanner.Bytes())
		if len(lineBytes) == 0 || lineBytes[0] == '#' || lineBytes[0] == '!' {
			continue
		}
		if bytes.HasPrefix(lineBytes, []byte("@@")) {
			continue
		}
		d := parseDomainLine(string(lineBytes))
		if d != "" {
			domains[d] = struct{}{}
		}
	}
	if err := scanner.Err(); err != nil {
		return 0, fmt.Errorf("scan input: %w", err)
	}

	count := len(domains)
	if count == 0 {
		return 0, fmt.Errorf("no valid domains found in filter list")
	}

	// Build trie
	root := newTrieBuilderNode()
	for d := range domains {
		root.insert(d)
	}
	if err := root.saveToFile(triePath); err != nil {
		return 0, fmt.Errorf("write trie: %w", err)
	}

	// Build bloom filter
	bloomFilter := bloom.NewBloomBuilder(count, 0.001)
	for d := range domains {
		bloomFilter.Add(d)
	}
	if err := bloomFilter.SaveToFile(bloomPath); err != nil {
		return 0, fmt.Errorf("write bloom: %w", err)
	}

	fmt.Fprintf(os.Stderr, "[BlockAds/Go] Compiled %d domains → %s + %s\n", count, triePath, bloomPath)
	return count, nil
}

func parseDomainLine(line string) string {
	line = strings.TrimSpace(line)

	if line == "" || line[0] == '#' || line[0] == '!' {
		return ""
	}

	if strings.HasPrefix(line, "@@") {
		return ""
	}

	// Cosmetic and scriptlet rules are not domains
	if strings.Contains(line, "##") || strings.Contains(line, "#@#") || strings.Contains(line, "#?#") ||
		strings.Contains(line, "#$#") || strings.Contains(line, "#%#") {
		return ""
	}

	// Support Wildcard domain format: *.example.com (HaGeZi wildcard, OISD domainswild)
	if strings.HasPrefix(line, "*.") {
		line = strings.TrimPrefix(line, "*.")
	}

	// dnsmasq format: server=/domain.com/ or address=/domain.com/0.0.0.0
	if strings.HasPrefix(line, "server=/") || strings.HasPrefix(line, "address=/") {
		clean := strings.TrimPrefix(strings.TrimPrefix(line, "server=/"), "address=/")
		if slashIdx := strings.IndexByte(clean, '/'); slashIdx != -1 {
			line = clean[:slashIdx]
		}
	}

	if strings.ContainsAny(line, "$/\\*") && !strings.HasPrefix(line, "||") {
		return ""
	}

	var domain string
	isABP := strings.HasPrefix(line, "||")

	switch {
	case isABP:
		domain = strings.TrimPrefix(line, "||")

		// Handle wildcard prefixes: ||*.example.com^ or ||*.tld^
		domain = strings.TrimPrefix(domain, "*.")
		domain = strings.TrimPrefix(domain, "*")

		// If the rule contains wildcards (*, ?) or paths (/), it's a URL-level rule.
		// A DNS-level blocker cannot handle this and will over-block if suffix is stripped.
		if strings.ContainsAny(domain, "/*?") {
			return ""
		}

		// Handle modifiers after $
		if dollarIdx := strings.IndexByte(domain, '$'); dollarIdx != -1 {
			options := domain[dollarIdx+1:]
			for _, opt := range strings.Split(options, ",") {
				opt = strings.TrimSpace(opt)
				if opt == "" || opt == "important" || opt == "empty" || opt == "mp4" ||
					opt == "all" || opt == "popup" || opt == "document" || opt == "doc" ||
					opt == "dns" || strings.HasPrefix(opt, "dnstype") || opt == "cname" ||
					opt == "network" || strings.HasPrefix(opt, "ctag") || strings.HasPrefix(opt, "denyallow") {
					continue
				}
				return ""
			}
			domain = domain[:dollarIdx]
		}

		if idx := strings.IndexByte(domain, '^'); idx != -1 {
			domain = domain[:idx]
		}

	case strings.HasPrefix(line, "0.0.0.0 "),
		strings.HasPrefix(line, "127.0.0.1 "):
		fields := strings.Fields(line)
		if len(fields) >= 2 {
			domain = fields[1]
		}
		if idx := strings.IndexByte(domain, '#'); idx != -1 {
			domain = domain[:idx]
		}

	default:
		if !strings.ContainsAny(line, " \t") && strings.Contains(line, ".") {
			domain = line
		}
	}

	domain = strings.TrimSpace(strings.ToLower(domain))

	if domain == "" || domain == "localhost" || domain == "localhost.localdomain" ||
		domain == "local" || domain == "broadcasthost" {
		return ""
	}

	// Single-label domains are rejected unless they come from an ABP TLD rule (e.g. ||*.actor^)
	if !strings.Contains(domain, ".") && !isABP {
		return ""
	}

	// Reject IP addresses
	if domain[0] >= '0' && domain[0] <= '9' {
		allDigitsOrDots := true
		for _, r := range domain {
			if (r < '0' || r > '9') && r != '.' {
				allDigitsOrDots = false
				break
			}
		}
		if allDigitsOrDots && net.ParseIP(domain) != nil {
			return ""
		}
	}

	// Validate valid hostname labels [a-z0-9-_]
	labels := strings.Split(domain, ".")
	if len(labels) < 2 && !isABP {
		return ""
	}
	for _, label := range labels {
		if label == "" {
			return ""
		}
		for i, r := range label {
			if (r < 'a' || r > 'z') && (r < '0' || r > '9') && r != '-' && r != '_' {
				return ""
			}
			if (r == '-' || r == '_') && (i == 0 || i == len(label)-1) {
				return ""
			}
		}
	}

	return domain
}

type trieBuilderNode struct {
	children   map[string]*trieBuilderNode
	isTerminal bool
}

func newTrieBuilderNode() *trieBuilderNode {
	return &trieBuilderNode{children: make(map[string]*trieBuilderNode)}
}

func (n *trieBuilderNode) insert(domain string) {
	labels := strings.Split(domain, ".")
	node := n
	for i := len(labels) - 1; i >= 0; i-- {
		label := labels[i]
		child, ok := node.children[label]
		if !ok {
			child = newTrieBuilderNode()
			node.children[label] = child
		}
		node = child
	}
	node.isTerminal = true
}

func (n *trieBuilderNode) saveToFile(path string) error {
	type bfsEntry struct {
		node   *trieBuilderNode
		offset int
	}

	nodeCount := 0
	domainCount := 0
	var countNodes func(node *trieBuilderNode)
	countNodes = func(node *trieBuilderNode) {
		nodeCount++
		if node.isTerminal {
			domainCount++
		}
		for _, child := range node.children {
			countNodes(child)
		}
	}
	countNodes(n)

	queue := []bfsEntry{{node: n, offset: headerSize}}
	currentOffset := headerSize
	offsets := make(map[*trieBuilderNode]int)

	for len(queue) > 0 {
		entry := queue[0]
		queue = queue[1:]

		offsets[entry.node] = currentOffset

		nodeSize := 1 + 4
		sortedLabels := sortedKeys(entry.node.children)
		for _, label := range sortedLabels {
			nodeSize += 2 + len(label) + 4
		}
		currentOffset += nodeSize

		for _, label := range sortedLabels {
			child := entry.node.children[label]
			queue = append(queue, bfsEntry{node: child})
		}
	}

	f, err := os.Create(path)
	if err != nil {
		return fmt.Errorf("create trie file: %w", err)
	}
	defer f.Close()

	header := make([]byte, headerSize)
	binary.BigEndian.PutUint32(header[0:4], trieMagic)
	binary.BigEndian.PutUint32(header[4:8], trieVersion)
	binary.BigEndian.PutUint32(header[8:12], uint32(nodeCount))
	binary.BigEndian.PutUint32(header[12:16], uint32(domainCount))
	if _, err := f.Write(header); err != nil {
		return err
	}

	queue2 := []*trieBuilderNode{n}
	for len(queue2) > 0 {
		node := queue2[0]
		queue2 = queue2[1:]

		if node.isTerminal {
			f.Write([]byte{1})
		} else {
			f.Write([]byte{0})
		}

		sortedLabels := sortedKeys(node.children)
		countBuf := make([]byte, 4)
		binary.BigEndian.PutUint32(countBuf, uint32(len(sortedLabels)))
		f.Write(countBuf)

		for _, label := range sortedLabels {
			child := node.children[label]

			lenBuf := make([]byte, 2)
			binary.BigEndian.PutUint16(lenBuf, uint16(len(label)))
			f.Write(lenBuf)
			f.Write([]byte(label))

			offBuf := make([]byte, 4)
			binary.BigEndian.PutUint32(offBuf, uint32(offsets[child]))
			f.Write(offBuf)
		}

		for _, label := range sortedLabels {
			queue2 = append(queue2, node.children[label])
		}
	}

	return nil
}

func sortedKeys(m map[string]*trieBuilderNode) []string {
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	return keys
}
