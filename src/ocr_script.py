import easyocr
import sys
import warnings
import os
from PIL import Image, ImageEnhance, ImageFilter

warnings.filterwarnings('ignore')
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
sys.stderr = open(os.devnull, 'w')

# ✅ Preprocessing avec Pillow
img = Image.open(sys.argv[1])

# Agrandir x2
img = img.resize((img.width * 2, img.height * 2), Image.LANCZOS)

# Augmenter le contraste
img = ImageEnhance.Contrast(img).enhance(2.5)

# Augmenter la netteté
img = ImageEnhance.Sharpness(img).enhance(2.0)

# Convertir en noir et blanc
img = img.convert('L')

# Sauver l'image prétraitée
processed_path = sys.argv[1] + "_processed.png"
img.save(processed_path)

reader = easyocr.Reader(['fr', 'en'], gpu=False, verbose=False)
results = reader.readtext(
    processed_path,
    detail=0,
    paragraph=True,
    contrast_ths=0.05,
    adjust_contrast=0.8
)

os.remove(processed_path)
print(' '.join(results))