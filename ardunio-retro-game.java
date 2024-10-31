#include <Wire.h>
#include <Adafruit_SSD1306.h>
#include <Adafruit_GFX.h>
#include <TM1637Display.h> 

#define OLED_GENISLIK 128
#define OLED_YUKSEKLIK 64
#define OLED_ADRES   0x3C

#define BTN_BASLA 5  
#define BTN_CIKIS 6   

Adafruit_SSD1306 ekran(OLED_GENISLIK, OLED_YUKSEKLIK);
TM1637Display ekranTM1637(2, 3); 

const int potPin = A0;
const int hayatLEDleri[] = {2, 3, 4}; 
long randNumara; 
int paletPozisyonu = 0;
int oncekiPaletPozisyonu = 0;
float topX = OLED_GENISLIK / 2; 
float topY = OLED_YUKSEKLIK / 2; 
float oncekiTopX = OLED_GENISLIK / 2; 
float oncekiTopY = OLED_YUKSEKLIK / 2; 
float topDX = 1.0; 
int topDY = 1; 
float oncekiTopDX = 1.0; 
int oncekiTopDY = 1; 
int blokBoyutuX = 10; 
int blokBoyutuY = 5; 
int canlar = 3;
bool bloklar[4][5]; 
int puan = 0; 

bool oyunBasladi = false;

struct GucuYukari {
  float x;
  float y;
  float hiz;
  bool aktif;
};

GucuYukari gucUp;

void setup() {
  pinMode(BTN_BASLA, INPUT_PULLUP);
  pinMode(BTN_CIKIS, INPUT_PULLUP);
  randomSeed(analogRead(0));
  ekran.begin(SSD1306_SWITCHCAPVCC, OLED_ADRES);
  ekran.clearDisplay();

  ekran.setTextSize(2);
  ekran.setTextColor(WHITE);
  ekran.setCursor(0, 0);
  ekran.println("prolab 2");

  ekran.setTextSize(1);
  ekran.setCursor(0, 30);
  ekran.println("Baslamak icin BASLAya basin");
  ekran.println("Cikmak icin CIKISa basin");

  
  for (int satir = 0; satir < 4; satir++) {
    for (int sutun = 0; sutun < 5; sutun++) {
      bloklar[satir][sutun] = true; 
    }
  }

  for (int i = 0; i < canlar; i++) {
    pinMode(hayatLEDleri[i], OUTPUT);
    digitalWrite(hayatLEDleri[i], HIGH); // LED'i aç
  }

  ekranTM1637.setBrightness(0x0f); // Parlaklığı maksimuma ayarla
  ekran.display();
}

void loop() {
  if (!oyunBasladi) {
    if (digitalRead(BTN_BASLA) == LOW) {
      oyunuBaslat();
    } else if (digitalRead(BTN_CIKIS) == LOW) {
      oyunuCikis();
    }
  } else {
    
    oyunuGuncelle();
    oyunuCiz();
  }
}

void oyunuBaslat() {
  ekran.clearDisplay();
  ekran.setTextSize(2);
  ekran.setTextColor(WHITE);
  ekran.setCursor(0, 0);
  ekran.println("Oyun Basladi");
  ekran.display();
  delay(1000); 
  oyunBasladi = true;
}

void oyunuCikis() {
  ekran.clearDisplay();
  ekran.setTextSize(2);
  ekran.setTextColor(WHITE);
  ekran.setCursor(0, 0);
  ekran.println("cikiliyor...");
  ekran.display();
  delay(1000); 
  while (true) {} 
}

void oyunuGuncelle() {
  
  oncekiPaletPozisyonu = paletPozisyonu;
  paletPozisyonu = map(analogRead(potPin), 0, 1023, 0, OLED_GENISLIK - 40); 

  
  oncekiTopX = topX;
  oncekiTopY = topY;
  oncekiTopDX = topDX;
  oncekiTopDY = topDY;
  topX += topDX;
  topY += topDY;

  
  if (topY >= OLED_YUKSEKLIK - 4 && topX >= paletPozisyonu && topX <= paletPozisyonu + 40) {
    
    float paletVekX = paletPozisyonu - oncekiPaletPozisyonu;

    
    topDY = -topDY;
    topDX += paletVekX * 0.2;
  }

  
  int topSatir = topY / blokBoyutuY;
  int topSutun = (topX - (OLED_GENISLIK - 5 * blokBoyutuX) / 2) / blokBoyutuX;
  if (topY >= 0 && topSatir < 4 && topSutun >= 0 && topSutun < 5 && bloklar[topSatir][topSutun]) {
    bloklar[topSatir][topSutun] = false; 

    
    
    topDX = -topDX;
    topDY = -topDY;
    puan++;
    ekranTM1637.showNumberDec(puan, false);
    randNumara = random(1,100);
    
    if(randNumara <=10){
    gucUpOlustur((OLED_GENISLIK - 5 * blokBoyutuX) / 2 + topSutun * blokBoyutuX + 2, topSatir * blokBoyutuY + 5, 1.0);
    }
  }

 
  if (topX <= 0 || topX >= OLED_GENISLIK) {
    topDX = -topDX;
  }
  if (topY <= 0) {
    topDY = -topDY;
  }

 
  if (topY >= OLED_YUKSEKLIK && canlar > 0) {
    topX = OLED_GENISLIK / 2;
    topY = OLED_YUKSEKLIK / 2;
    canlar--;
    digitalWrite(hayatLEDleri[canlar], LOW);
  } else if (canlar <= 0) {
     ekran.print("Haklarin tükendi!");
    oyunBasladi = false; // Oyunu bitir
   
  }


  if (puan == 20) {
    topDX *= 1.2;
    topDY *= 1.2;
    topX = OLED_GENISLIK / 2;
    topY = OLED_YUKSEKLIK / 2;
    bloklariSifirla(); 
    puan = 0; 
    ekranTM1637.showNumberDec(puan, false);
  }

  
  if (gucUp.aktif) {
    gucUp.y += gucUp.hiz;

    
    if (gucUp.y + 5 >= OLED_YUKSEKLIK - 4 && gucUp.x >= paletPozisyonu && gucUp.x <= paletPozisyonu + 40) {
      
      if (canlar < 3) {
        canlar++;
        digitalWrite(hayatLEDleri[canlar - 1], HIGH); // LED'i aç
      }
      gucUp.aktif = false; 
    }

  
    if (gucUp.y >= OLED_YUKSEKLIK) {
      gucUp.aktif = false; 
    }
  }
}

void oyunuCiz() {
  ekran.clearDisplay();

  
  for (int satir = 0; satir < 4; satir++) {
    for (int sutun = 0; sutun < 5; sutun++) {
      if (bloklar[satir][sutun]) { 
        int x = (OLED_GENISLIK - 5 * blokBoyutuX) / 2 + sutun * blokBoyutuX;
        int y = satir * blokBoyutuY;
        ekran.drawRect(x, y, blokBoyutuX, blokBoyutuY, WHITE); 
        ekran.fillRect(x + 1, y + 1, blokBoyutuX - 2, blokBoyutuY - 2, BLACK); 
      }
    }
  }

 
  int paletGenislik = 40; 
  int paletYukseklik = 4; 
  ekran.fillRect(paletPozisyonu, OLED_YUKSEKLIK - paletYukseklik, paletGenislik, paletYukseklik, WHITE);

 
  ekran.drawCircle(topX, topY, 2, WHITE);

  
  if (gucUp.aktif) {
    ekran.fillRect(gucUp.x, gucUp.y, 5, 5, WHITE); 
  }

  ekran.display();
}

void bloklariSifirla() {
 
  for (int satir = 0; satir < 4; satir++) {
    for (int sutun = 0; sutun < 5; sutun++) {
      bloklar[satir][sutun] = true;
    }
  }
}


void gucUpOlustur(float x, float y, float hiz) {
  gucUp.x = x;
  gucUp.y = y;
  gucUp.hiz = hiz;
  gucUp.aktif = true;
}
