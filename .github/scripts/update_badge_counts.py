import urllib.request
import json
import re
import os

REPO = "Bn5prS/Photon_Gallery"

def fetch_stats():
    stars = 48
    downloads = 3000
    views = "3.8K+"
    
    # 1. Fetch Stars
    try:
        req = urllib.request.Request(f"https://api.github.com/repos/{REPO}", headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=10) as r:
            data = json.loads(r.read().decode("utf-8"))
            stars = data.get("stargazers_count", stars)
    except Exception as e:
        print("Stars fetch error:", e)

    # 2. Fetch Downloads
    try:
        req = urllib.request.Request(f"https://api.github.com/repos/{REPO}/releases", headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=10) as r:
            releases = json.loads(r.read().decode("utf-8"))
            total = sum(asset.get("download_count", 0) for rel in releases for asset in rel.get("assets", []))
            if total > 0:
                downloads = total
    except Exception as e:
        print("Downloads fetch error:", e)

    # 3. Fetch Views
    try:
        req = urllib.request.Request(f"https://hits.sh/github.com/{REPO}.svg", headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=10) as r:
            svg = r.read().decode("utf-8")
            m = re.search(r'aria-label="VIEWS:\s*([^"]+)"', svg)
            if m:
                views = m.group(1).replace(",", "")
                # Format to e.g. 3.8K+
                v_num = int(views)
                if v_num >= 1000:
                    views = f"{v_num / 1000:.1f}K+"
                else:
                    views = str(v_num)
    except Exception as e:
        print("Views fetch error:", e)

    # Format numbers
    stars_str = f"{stars} Stars"
    if downloads >= 1000:
        dl_str = f"{downloads / 1000:.1f}K+ Downloads" if downloads % 1000 >= 100 else f"{downloads // 1000}K+ Downloads"
    else:
        dl_str = f"{downloads} Downloads"
    views_str = f"{views} Views" if "Views" not in str(views) else str(views)

    return stars_str, dl_str, views_str

def update_badge(badge_id, label, icon_svg_path, dark_theme, light_theme):
    w = int(38 + len(label) * 7.4 + 14)
    h = 36
    rx = 18
    for mode, th in [("dark", dark_theme), ("light", light_theme)]:
        icon_str = icon_svg_path.format(color=th["icon"])
        svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
  <rect width="{w}" height="{h}" rx="{rx}" fill="{th['bg']}" stroke="{th['border']}" stroke-width="1.2"/>
  <svg x="12" y="9" width="18" height="18" viewBox="0 0 24 24">
    {icon_str}
  </svg>
  <text x="38" y="22.5" font-family="system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif" font-size="12.5" font-weight="600" fill="{th['text']}">{label}</text>
</svg>'''
        path = f"assets/readme/{mode}/{badge_id}.svg"
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(svg)

if __name__ == "__main__":
    stars_label, dl_label, views_label = fetch_stats()
    print(f"Stats: {stars_label} | {dl_label} | {views_label}")
    
    star_icon = '<path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" fill="{color}"/>'
    dl_icon = '<path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z" fill="{color}"/>'
    view_icon = '<path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" fill="{color}"/>'
    
    update_badge("badge-stars", stars_label, star_icon,
                 {"bg": "#251E14", "border": "#3B3120", "icon": "#FFB951", "text": "#F5E0C2"},
                 {"bg": "#FFF8F0", "border": "#F0DEC8", "icon": "#785900", "text": "#281A00"})

    update_badge("badge-downloads", dl_label, dl_icon,
                 {"bg": "#14221B", "border": "#20382D", "icon": "#8BF8CA", "text": "#CBEBDD"},
                 {"bg": "#F0FAF5", "border": "#C6E7D7", "icon": "#006C4C", "text": "#002114"})

    update_badge("badge-views", views_label, view_icon,
                 {"bg": "#142024", "border": "#1F353C", "icon": "#80D5E4", "text": "#C4E8EE"},
                 {"bg": "#F0F9FB", "border": "#C3E5EC", "icon": "#006874", "text": "#001F24"})
    
    print("Updated dynamic badge SVGs successfully!")

