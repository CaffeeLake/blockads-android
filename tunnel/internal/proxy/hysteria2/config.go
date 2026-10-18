package hysteria2

import (
	"encoding/json"
	"fmt"
	"net"
	"net/url"
	"strconv"
	"strings"
)

// Config represents a parsed Hysteria 2 configuration.
type Config struct {
	Server        string   `json:"server"`
	Port          int      `json:"port"`
	Password      string   `json:"password"`
	SNI           string   `json:"sni,omitempty"`
	AllowInsecure bool     `json:"allowInsecure,omitempty"`
	ALPN          []string `json:"alpn,omitempty"`
	Obfs          string   `json:"obfs,omitempty"`
	ObfsPassword  string   `json:"obfsPassword,omitempty"`
	Tag           string   `json:"tag,omitempty"`
}

// ToJSON serializes the config to a JSON string.
func (c *Config) ToJSON() string {
	b, _ := json.Marshal(c)
	return string(b)
}

// FromJSON parses a JSON string into a Config.
func FromJSON(data string) (*Config, error) {
	var c Config
	if err := json.Unmarshal([]byte(data), &c); err != nil {
		return nil, err
	}
	if c.Server == "" || c.Port <= 0 {
		return nil, fmt.Errorf("invalid hysteria2 config: missing required fields")
	}
	if c.SNI == "" {
		c.SNI = c.Server
	}
	if len(c.ALPN) == 0 {
		c.ALPN = []string{"h3"}
	}
	return &c, nil
}

// ParseURI parses standard hysteria2:// and hy2:// URIs.
func ParseURI(rawURI string) (*Config, error) {
	rawURI = strings.TrimSpace(rawURI)
	if !strings.HasPrefix(rawURI, "hysteria2://") && !strings.HasPrefix(rawURI, "hy2://") {
		return nil, fmt.Errorf("invalid scheme: URI must start with hysteria2:// or hy2://")
	}

	u, err := url.Parse(rawURI)
	if err != nil {
		return nil, fmt.Errorf("invalid URL: %w", err)
	}

	password := ""
	if u.User != nil {
		password = u.User.Username()
	}

	host, portStr, err := net.SplitHostPort(u.Host)
	if err != nil {
		host = u.Host
		portStr = "443"
	}
	port, _ := strconv.Atoi(portStr)
	if port <= 0 {
		port = 443
	}

	q := u.Query()
	sni := q.Get("sni")
	if sni == "" {
		sni = q.Get("peer")
	}
	if sni == "" {
		sni = host
	}

	allowInsecure := q.Get("allowInsecure") == "1" || strings.EqualFold(q.Get("allowInsecure"), "true") || strings.EqualFold(q.Get("insecure"), "1")

	var alpn []string
	if alpnStr := q.Get("alpn"); alpnStr != "" {
		for _, p := range strings.Split(alpnStr, ",") {
			if trimmed := strings.TrimSpace(p); trimmed != "" {
				alpn = append(alpn, trimmed)
			}
		}
	}
	if len(alpn) == 0 {
		alpn = []string{"h3"}
	}

	obfs := q.Get("obfs")
	obfsPassword := q.Get("obfs-password")
	if obfsPassword == "" {
		obfsPassword = q.Get("obfsPassword")
	}

	tag := u.Fragment
	if tag != "" {
		if unescaped, err := url.QueryUnescape(tag); err == nil {
			tag = unescaped
		}
	}

	return &Config{
		Server:        host,
		Port:          port,
		Password:      password,
		SNI:           sni,
		AllowInsecure: allowInsecure,
		ALPN:          alpn,
		Obfs:          obfs,
		ObfsPassword:  obfsPassword,
		Tag:           tag,
	}, nil
}
