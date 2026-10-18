package tunnel

import (
	"bufio"
	"fmt"
	"net"
	"net/http"
	"strings"
	"time"

	"github.com/nqmgaming/blockads-tunnel/internal/proxy"
	"github.com/nqmgaming/blockads-tunnel/internal/proxy/hysteria2"
	"github.com/nqmgaming/blockads-tunnel/internal/proxy/masque"
	"github.com/nqmgaming/blockads-tunnel/internal/proxy/trojan"
	"github.com/nqmgaming/blockads-tunnel/internal/proxy/tuic"
	"github.com/nqmgaming/blockads-tunnel/internal/proxy/vless"
	"github.com/nqmgaming/blockads-tunnel/internal/proxy/vmess"
	"github.com/nqmgaming/blockads-tunnel/internal/shadowsocks"
)

// TestProxyNodeDelay measures real-world HTTP 204 response delay through a proxy node.
// Returns the round-trip latency in milliseconds, or -1 and error if the test fails.
func TestProxyNodeDelay(protocol string, configJSON string, timeoutMs int) (int64, error) {
	return testProxyNodeDelayWithProtect(protocol, configJSON, timeoutMs, nil)
}

// TestProxyNodeDelayProtected measures HTTP 204 response delay through a proxy node with socket protection.
func TestProxyNodeDelayProtected(protocol string, configJSON string, timeoutMs int, protector SocketProtector) (int64, error) {
	var protectFn func(fd int) bool
	if protector != nil {
		protectFn = func(fd int) bool { return protector.Protect(fd) }
	}
	return testProxyNodeDelayWithProtect(protocol, configJSON, timeoutMs, protectFn)
}

// TestProxyDelay on Engine uses the engine's protect function to ensure test packets bypass VPN tunnel.
func (e *Engine) TestProxyDelay(protocol string, configJSON string, timeoutMs int) (int64, error) {
	return testProxyNodeDelayWithProtect(protocol, configJSON, timeoutMs, e.protectFn)
}

func testProxyNodeDelayWithProtect(protocol string, configJSON string, timeoutMs int, protectFn func(fd int) bool) (int64, error) {
	protocol = strings.ToLower(strings.TrimSpace(protocol))
	if configJSON == "" || protocol == "" {
		return -1, fmt.Errorf("empty protocol or config")
	}

	if timeoutMs <= 0 {
		timeoutMs = 5000
	}
	timeout := time.Duration(timeoutMs) * time.Millisecond

	var client proxy.Client
	var err error

	switch protocol {
	case "shadowsocks", "ss":
		cfg, parseErr := shadowsocks.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = shadowsocks.NewClient(cfg, protectFn)

	case "trojan":
		cfg, parseErr := trojan.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = trojan.NewClient(cfg, protectFn)

	case "vless":
		cfg, parseErr := vless.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = vless.NewClient(cfg, protectFn)

	case "vmess":
		cfg, parseErr := vmess.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = vmess.NewClient(cfg, protectFn)

	case "hysteria2", "hy2":
		cfg, parseErr := hysteria2.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = hysteria2.NewClient(cfg, protectFn)

	case "tuic":
		cfg, parseErr := tuic.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = tuic.NewClient(cfg, protectFn)

	case "masque":
		cfg, parseErr := masque.FromJSON(configJSON)
		if parseErr != nil {
			return -1, parseErr
		}
		client, err = masque.NewClient(cfg, protectFn)

	default:
		return -1, fmt.Errorf("unsupported protocol: %s", protocol)
	}

	if err != nil {
		return -1, fmt.Errorf("init client failed: %w", err)
	}
	if closer, ok := client.(interface{ Close() error }); ok {
		defer closer.Close()
	}

	// Anycast Cloudflare IP for cp.cloudflare.com
	targetIP := net.ParseIP("104.16.132.229")
	start := time.Now()

	// Dial through the proxy client
	conn, err := client.DialTCP(targetIP, 80)
	if err != nil {
		return -1, fmt.Errorf("dial proxy failed: %w", err)
	}
	defer conn.Close()

	_ = conn.SetDeadline(time.Now().Add(timeout))

	req := "GET /generate_204 HTTP/1.1\r\nHost: cp.cloudflare.com\r\nUser-Agent: BlockAds/1.0\r\nConnection: close\r\n\r\n"
	if _, err := conn.Write([]byte(req)); err != nil {
		return -1, fmt.Errorf("write http request failed: %w", err)
	}

	resp, err := http.ReadResponse(bufio.NewReader(conn), nil)
	if err != nil {
		return -1, fmt.Errorf("read http response failed: %w", err)
	}
	_ = resp.Body.Close()

	if resp.StatusCode != 204 && resp.StatusCode != 200 {
		return -1, fmt.Errorf("unexpected http status: %d", resp.StatusCode)
	}

	latency := time.Since(start).Milliseconds()
	return latency, nil
}
