import os

files = {
    'app/src/main/res/drawable/ic_launcher_background.xml': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#D32F2F"
        android:pathData="M0,0h108v108h-108z" />
</vector>
''',

    'app/src/main/res/drawable/ic_launcher_foreground.xml': '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M34,34 h40 c4,0 6,2 6,6 v25 c0,4 -2,6 -6,6 h-20 l-14,14 v-14 h-6 c-4,0 -6,-2 -6,-6 v-25 c0,-4 2,-6 6,-6 z" />
</vector>
''',

    'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml': '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
''',

    'app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml': '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
'''
}

for filepath, content in files.items():
    os.makedirs(os.path.dirname(filepath) if os.path.dirname(filepath) else '.', exist_ok=True)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
print("Icons created.")
