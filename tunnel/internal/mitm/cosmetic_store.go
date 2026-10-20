package mitm

import (
	"strings"
	"sync"
)

// CosmeticStore organizes cosmetic CSS rules into generic rules and per-host rules.
type CosmeticStore struct {
	mu         sync.RWMutex
	genericCSS string
	byHost     map[string][]string
}

// Global cosmetic store singleton
var defaultCosmeticStore = NewCosmeticStore()

// NewCosmeticStore creates an empty cosmetic store.
func NewCosmeticStore() *CosmeticStore {
	return &CosmeticStore{
		byHost: make(map[string][]string),
	}
}

// ParseAndLoad parses cosmetic CSS rules from raw filter list text.
// Supports both standard AdBlock cosmetic rules (domain##selector, ##genericSelector)
// and raw CSS blocks.
func (s *CosmeticStore) ParseAndLoad(content string) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.byHost = make(map[string][]string)
	var genericBuilder strings.Builder

	lines := strings.Split(content, "\n")
	hasAdblockRules := false

	for _, line := range lines {
		trimmed := strings.TrimSpace(line)
		if trimmed == "" || strings.HasPrefix(trimmed, "!") || strings.HasPrefix(trimmed, "[") {
			continue
		}

		// AdBlock cosmetic rule: [domains]##selector
		if idx := strings.Index(trimmed, "##"); idx >= 0 && !strings.Contains(trimmed, "##+js(") {
			hasAdblockRules = true
			domainPart := trimmed[:idx]
			selector := strings.TrimSpace(trimmed[idx+2:])
			if selector == "" {
				continue
			}

			ruleCSS := selector + " { display: none !important; }"

			if domainPart == "" {
				genericBuilder.WriteString(ruleCSS)
				genericBuilder.WriteString("\n")
			} else {
				for _, d := range strings.Split(domainPart, ",") {
					d = strings.ToLower(strings.TrimSpace(d))
					if d != "" && !strings.HasPrefix(d, "~") {
						s.byHost[d] = append(s.byHost[d], ruleCSS)
					}
				}
			}
		}
	}

	// If no standard ## rules were found, treat the content as raw generic CSS
	if !hasAdblockRules {
		s.genericCSS = content
	} else {
		s.genericCSS = genericBuilder.String()
	}

	logf("CosmeticStore updated: generic rules=%d bytes, hosts=%d", len(s.genericCSS), len(s.byHost))
}

// AddHostCSS adds custom CSS specifically for a domain (e.g. from RuleManifest).
func (s *CosmeticStore) AddHostCSS(domain string, css string) {
	s.mu.Lock()
	defer s.mu.Unlock()

	domain = strings.ToLower(strings.TrimSpace(domain))
	if domain != "" && css != "" {
		s.byHost[domain] = append(s.byHost[domain], css)
	}
}

// GetCSSForHost returns all cosmetic CSS applicable to the specific hostname.
func (s *CosmeticStore) GetCSSForHost(host string) string {
	s.mu.RLock()
	defer s.mu.RUnlock()

	host = normalizeHost(host)
	var rules []string

	for d, cssList := range s.byHost {
		if host == d || strings.HasSuffix(host, "."+d) {
			rules = append(rules, cssList...)
		}
	}

	if len(rules) == 0 {
		return ""
	}
	return strings.Join(rules, "\n")
}

// GetGenericCSS returns the generic CSS rules applied globally.
func (s *CosmeticStore) GetGenericCSS() string {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.genericCSS
}

// SetCosmeticStoreRules replaces the active cosmetic rules in the default store.
func SetCosmeticStoreRules(content string) {
	defaultCosmeticStore.ParseAndLoad(content)
}

// GetHostCosmeticCSS returns CSS for host from the default store.
func GetHostCosmeticCSS(host string) string {
	return defaultCosmeticStore.GetCSSForHost(host)
}
