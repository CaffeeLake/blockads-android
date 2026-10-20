package mitm

import (
	"net/http"
	"net/url"
	"testing"
)

func TestModuleStore_MatchAndHandle(t *testing.T) {
	rawJSON := `{
		"version": 1,
		"modules": [
			{
				"id": "youtube",
				"name": "YouTube",
				"enabled": true,
				"domains": ["youtube.com", "m.youtube.com"],
				"mock_paths": [
					{
						"pattern": "/youtubei/v1/player/ad_break",
						"status": 200,
						"contentType": "application/json",
						"body": "{}"
					},
					{
						"pattern": "/api/stats/ads",
						"status": 204
					}
				],
				"block_paths": ["/pagead/ad_status"],
				"cosmetic_css": [".ad-showing { display: none !important; }"],
				"scripts": ["alert(1);"]
			}
		],
		"domainIndex": {
			"youtube.com": "youtube",
			"m.youtube.com": "youtube"
		},
		"globalBlockPaths": ["/global/track"]
	}`

	store := &ModuleStore{
		modules:     make(map[string]*WebModule),
		domainIndex: make(map[string]*WebModule),
	}

	if err := store.UpdateFromJSON(rawJSON); err != nil {
		t.Fatalf("UpdateFromJSON failed: %v", err)
	}

	// 1. Mock path with 200 & body
	req1 := &http.Request{
		Method: "POST",
		URL:    &url.URL{Path: "/youtubei/v1/player/ad_break"},
	}
	resp1, handled1 := store.MatchAndHandle(req1, "m.youtube.com")
	if !handled1 || resp1 == nil {
		t.Fatalf("expected /youtubei/v1/player/ad_break to be handled")
	}
	if resp1.StatusCode != 200 {
		t.Errorf("expected status 200, got %d", resp1.StatusCode)
	}
	if resp1.Header.Get("Content-Type") != "application/json" {
		t.Errorf("expected Content-Type application/json, got %s", resp1.Header.Get("Content-Type"))
	}

	// 2. Mock path with 204
	req2 := &http.Request{
		Method: "GET",
		URL:    &url.URL{Path: "/api/stats/ads"},
	}
	resp2, handled2 := store.MatchAndHandle(req2, "youtube.com")
	if !handled2 || resp2.StatusCode != 204 {
		t.Fatalf("expected /api/stats/ads to be mock 204")
	}

	// 3. Module block path
	req3 := &http.Request{
		Method: "GET",
		URL:    &url.URL{Path: "/pagead/ad_status?id=1"},
	}
	resp3, handled3 := store.MatchAndHandle(req3, "youtube.com")
	if !handled3 || resp3.StatusCode != 204 {
		t.Fatalf("expected /pagead/ad_status to be blocked with 204")
	}

	// 4. Global block path on unrelated domain
	req4 := &http.Request{
		Method: "GET",
		URL:    &url.URL{Path: "/global/track/pixel.gif"},
	}
	resp4, handled4 := store.MatchAndHandle(req4, "random-news.com")
	if !handled4 || resp4.StatusCode != 204 {
		t.Fatalf("expected global path to be blocked on any host")
	}

	// 5. Normal unblocked request
	req5 := &http.Request{
		Method: "GET",
		URL:    &url.URL{Path: "/watch?v=123"},
	}
	_, handled5 := store.MatchAndHandle(req5, "youtube.com")
	if handled5 {
		t.Fatalf("expected normal request to NOT be handled by module store")
	}

	// 6. Test Cosmetic CSS & Scripts lookups
	css := store.GetCosmeticCSS("m.youtube.com")
	if len(css) != 1 || css[0] != ".ad-showing { display: none !important; }" {
		t.Errorf("unexpected cosmetic CSS: %v", css)
	}

	scripts := store.GetScripts("youtube.com")
	if len(scripts) != 1 || scripts[0] != "alert(1);" {
		t.Errorf("unexpected scripts: %v", scripts)
	}
}
