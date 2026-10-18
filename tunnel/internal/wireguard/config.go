package wireguard

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"
)

// WgConfig represents the JSON format of a WireGuard configuration.
// Must match Kotlin's WireGuardConfig serialization format.
type WgConfig struct {
	Interface WgInterface `json:"interfaceConfig"`
	Peers     []WgPeer    `json:"peers"`
}

// WgInterface is the [Interface] section.
type WgInterface struct {
	PrivateKey string   `json:"privateKey"`
	Address    []string `json:"address"`
	ListenPort *int     `json:"listenPort,omitempty"`
	DNS        []string `json:"dns"`
	// AmneziaWG obfuscation parameters
	Jc   *int   `json:"jc,omitempty"`
	Jmin *int   `json:"jmin,omitempty"`
	Jmax *int   `json:"jmax,omitempty"`
	S1   *int   `json:"s1,omitempty"`
	S2   *int   `json:"s2,omitempty"`
	S3   *int   `json:"s3,omitempty"`
	S4   *int   `json:"s4,omitempty"`
	H1   *int64 `json:"h1,omitempty"`
	H2   *int64 `json:"h2,omitempty"`
	H3   *int64 `json:"h3,omitempty"`
	H4   *int64 `json:"h4,omitempty"`
}

// WgPeer is a [Peer] section.
type WgPeer struct {
	PublicKey           string   `json:"publicKey"`
	PresharedKey        *string  `json:"presharedKey,omitempty"`
	Endpoint            *string  `json:"endpoint,omitempty"`
	AllowedIPs          []string `json:"allowedIPs"`
	PersistentKeepalive *int     `json:"persistentKeepalive,omitempty"`
}

// ParseWgConfigJSON parses a JSON string into WgConfig.
func ParseWgConfigJSON(jsonStr string) (*WgConfig, error) {
	var cfg WgConfig
	if err := json.Unmarshal([]byte(jsonStr), &cfg); err != nil {
		return nil, fmt.Errorf("invalid wireguard json: %w", err)
	}
	return &cfg, nil
}

// BuildIpcConfig converts a WgConfig into wireguard-go UAPI IPC configuration format.
func BuildIpcConfig(cfg *WgConfig) (string, error) {
	var sb strings.Builder

	privHex, err := base64ToHex(cfg.Interface.PrivateKey)
	if err != nil {
		return "", fmt.Errorf("interface private_key: %w", err)
	}
	sb.WriteString(fmt.Sprintf("private_key=%s\n", privHex))

	if cfg.Interface.ListenPort != nil && *cfg.Interface.ListenPort > 0 {
		sb.WriteString(fmt.Sprintf("listen_port=%d\n", *cfg.Interface.ListenPort))
	}

	// AmneziaWG UAPI directives
	if cfg.Interface.Jc != nil && *cfg.Interface.Jc > 0 {
		sb.WriteString(fmt.Sprintf("jc=%d\n", *cfg.Interface.Jc))
	}
	if cfg.Interface.Jmin != nil && *cfg.Interface.Jmin > 0 {
		sb.WriteString(fmt.Sprintf("jmin=%d\n", *cfg.Interface.Jmin))
	}
	if cfg.Interface.Jmax != nil && *cfg.Interface.Jmax > 0 {
		sb.WriteString(fmt.Sprintf("jmax=%d\n", *cfg.Interface.Jmax))
	}
	if cfg.Interface.S1 != nil && *cfg.Interface.S1 >= 0 {
		sb.WriteString(fmt.Sprintf("s1=%d\n", *cfg.Interface.S1))
	}
	if cfg.Interface.S2 != nil && *cfg.Interface.S2 >= 0 {
		sb.WriteString(fmt.Sprintf("s2=%d\n", *cfg.Interface.S2))
	}
	if cfg.Interface.H1 != nil {
		sb.WriteString(fmt.Sprintf("h1=%d\n", *cfg.Interface.H1))
	}
	if cfg.Interface.H2 != nil {
		sb.WriteString(fmt.Sprintf("h2=%d\n", *cfg.Interface.H2))
	}
	if cfg.Interface.H3 != nil {
		sb.WriteString(fmt.Sprintf("h3=%d\n", *cfg.Interface.H3))
	}
	if cfg.Interface.H4 != nil {
		sb.WriteString(fmt.Sprintf("h4=%d\n", *cfg.Interface.H4))
	}

	for _, peer := range cfg.Peers {
		pubHex, err := base64ToHex(peer.PublicKey)
		if err != nil {
			return "", fmt.Errorf("peer public_key: %w", err)
		}
		sb.WriteString(fmt.Sprintf("public_key=%s\n", pubHex))

		if peer.PresharedKey != nil && *peer.PresharedKey != "" {
			pskHex, err := base64ToHex(*peer.PresharedKey)
			if err != nil {
				return "", fmt.Errorf("peer preshared_key: %w", err)
			}
			sb.WriteString(fmt.Sprintf("preshared_key=%s\n", pskHex))
		}

		if peer.Endpoint != nil && *peer.Endpoint != "" {
			sb.WriteString(fmt.Sprintf("endpoint=%s\n", *peer.Endpoint))
		}

		if peer.PersistentKeepalive != nil && *peer.PersistentKeepalive > 0 {
			sb.WriteString(fmt.Sprintf("persistent_keepalive_interval=%d\n", *peer.PersistentKeepalive))
		}

		for _, allowedIP := range peer.AllowedIPs {
			allowedIP = strings.TrimSpace(allowedIP)
			if allowedIP != "" {
				sb.WriteString(fmt.Sprintf("allowed_ip=%s\n", allowedIP))
			}
		}
	}

	return sb.String(), nil
}

func base64ToHex(b64 string) (string, error) {
	b64 = strings.TrimSpace(b64)
	bytes, err := base64.StdEncoding.DecodeString(b64)
	if err != nil {
		return "", fmt.Errorf("base64 decode failed: %w", err)
	}
	if len(bytes) != 32 {
		return "", fmt.Errorf("expected 32 bytes for key, got %d", len(bytes))
	}
	return hex.EncodeToString(bytes), nil
}
