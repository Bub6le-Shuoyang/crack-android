import os
import re

layout_dir = r"d:\desktop\monitor\app\src\main\res\layout"

def process_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    original = content

    # Remove hardcoded corner radius in TextInputLayout
    content = re.sub(r'\s*app:boxCornerRadiusTopStart="[^"]+"', '', content)
    content = re.sub(r'\s*app:boxCornerRadiusTopEnd="[^"]+"', '', content)
    content = re.sub(r'\s*app:boxCornerRadiusBottomStart="[^"]+"', '', content)
    content = re.sub(r'\s*app:boxCornerRadiusBottomEnd="[^"]+"', '', content)

    # Remove hardcoded button corner radius
    content = re.sub(r'\s*app:cornerRadius="[^"]+"', '', content)

    # Replace specific hardcoded styles with custom styles or default attributes
    # The default for TextInputLayout and Button will be picked up from the theme if we remove the style attribute entirely
    # or we can explicitly set style="?attr/textInputStyle"
    content = content.replace('style="@style/Widget.Material3.TextInputLayout.OutlinedBox"', 'style="?attr/textInputStyle"')
    content = content.replace('style="@style/Widget.Material3.Button"', 'style="?attr/materialButtonStyle"')

    # Keep TextButton style as is, but we might want to update its color
    
    if original != content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Updated: {os.path.basename(file_path)}")

for f in os.listdir(layout_dir):
    if f.endswith(".xml"):
        process_file(os.path.join(layout_dir, f))
