import os
from PIL import Image

def make_icons():
    source_image = r"c:\AndroidStudio\IlacTakip2\logo2.jpeg"
    base_res_dir = r"c:\AndroidStudio\IlacTakip2\app\src\main\res"

    try:
        img = Image.open(source_image).convert("RGBA")
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    # Mipmap (legacy) sizes
    sizes = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }

    # Foreground (adaptive) sizes
    fg_sizes = {
        "mdpi": 108,
        "hdpi": 162,
        "xhdpi": 216,
        "xxhdpi": 324,
        "xxxhdpi": 432
    }

    for dens, size in sizes.items():
        # Create mipmap dir if not exists
        mipmap_dir = os.path.join(base_res_dir, f"mipmap-{dens}")
        os.makedirs(mipmap_dir, exist_ok=True)

        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(os.path.join(mipmap_dir, "ic_launcher.png"))
        
        # Round icon, just scale it normally or mask it
        # For simplicity, we just save the square image as round icon, Android can mask it or we can leave it.
        # But it's better to make a circular mask for round if possible.
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_img = resized.copy()
        round_img.putalpha(mask)
        round_img.save(os.path.join(mipmap_dir, "ic_launcher_round.png"))

    for dens, size in fg_sizes.items():
        drawable_dir = os.path.join(base_res_dir, f"drawable-{dens}")
        os.makedirs(drawable_dir, exist_ok=True)
        
        resized_fg = img.resize((size, size), Image.Resampling.LANCZOS)
        # However, adaptive icons usually only have the logo in the center (72x72 safe zone for a 108x108 image)
        # So we should create a transparent 108x108 image and paste the logo in the center
        icon_size = int(size * 0.66) # scale logo to 66% of foreground
        logo_resized = img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        
        fg_img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
        offset = ((size - icon_size) // 2, (size - icon_size) // 2)
        fg_img.paste(logo_resized, offset)
        
        fg_img.save(os.path.join(drawable_dir, "ic_launcher_foreground.png"))

    print("Icons generated successfully.")

if __name__ == "__main__":
    make_icons()
