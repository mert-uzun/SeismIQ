import boto3
import os
import random
import datetime
from dotenv import load_dotenv

load_dotenv()

# DynamoDB
dynamodb = boto3.resource("dynamodb")
mock_data_table = dynamodb.Table(os.getenv("MOCK_DATA_TABLE_NAME"))
print("Mock data table: " + os.getenv("MOCK_DATA_TABLE_NAME"))
print("DynamoDB connection is secured.")

templates = [
    "{location} {need} {verb} {hashtags}",
    "{location} için {need} {verb} {hashtags}",
    "{location} acil {need} {verb} {hashtags}",
    "{location} bölgesinde {need} {verb} {hashtags}",
    "{location} civarında {need} {verb} {hashtags}",
    "{location} {need} {verb}. {hashtags}",
    "{location} {need} {verb} {filler} {hashtags}",
    "{location} {filler} {need} {verb} {hashtags}",
    "{location} için {filler} {need} {verb} {hashtags}",
    "{location} {need} {filler} {verb} {hashtags}",
    "{location} {filler} {verb} {need} {hashtags}",
    "{need} {location} {verb} {hashtags}",
    "{filler} {location} {need} {verb} {hashtags}",
    "{need} {verb} {location} {hashtags}",
    "{need} {verb} {filler} {location} {hashtags}",
    "{need} {verb} {hashtags} {location}",
    "{filler} {need} {verb} {hashtags} {location}",
    "{location} {need} {hashtags} {verb} {filler}",
    "{location} bölgesinde {filler} {need} {verb}, lütfen destek olun. {hashtags}",
    "{location} için {need} {verb}, {filler} yardım bekleniyor. {hashtags}",
    "{location} civarında {need} {verb}, {filler} acil ihtiyaç var. {hashtags}",
    "{location} acil {need} {verb}, {filler} destek gerekli. {hashtags}",
    "{location} bölgesinde {need} {verb}. {filler} {hashtags}",
    "{location} {need} {verb}, {filler} lütfen paylaşın. {hashtags}",
    "{location} için {need} {verb}, {filler} ulaşamıyoruz. {hashtags}",
    "{location} {need} {verb}, {filler} acil yardım çağrısı. {hashtags}",
    "{location} {filler} {need} {verb}, {hashtags} desteğe ihtiyacımız var.",
    "{location} {need} {verb}, {filler} yardım bekliyoruz. {hashtags}",
    "{location} {need} {verb}, {filler} acil destek lazım. {hashtags}",
    "{location} için {need} {verb} var mı? {hashtags}",
    "{location} bölgesinde {need} {verb} olan var mı? {hashtags}",
    "{location} {need} {verb} gönderebilecek var mı? {hashtags}",
    "{location} {need} {verb} ulaştırabilir misiniz? {hashtags}",
    "{location} {need} {verb} sağlayabilecek var mı? {hashtags}",
    "{location} {need} {verb} için destek arıyoruz. {hashtags}",
    "{location} {need} {verb} için gönüllü var mı? {hashtags}",
    "{location} {need} {verb} için yardım bekleniyor. {hashtags}",
    "{location} {need} {verb} için acil yardım çağrısı. {hashtags}",
    "{location} {need} {verb} kimse getiremiyor mu? {hashtags}",
    "{location} {need} {verb} nereden bulabiliriz? {hashtags}",
    "{location} {need} {verb} nasıl temin edebiliriz? {hashtags}",
    "{location} mahsur kaldık, {need} {verb} {filler} {hashtags}",
    "{location} yardım alamıyoruz, {need} {verb} {filler} {hashtags}",
    "{location} ailemle beraberiz, {need} {verb} {filler} {hashtags}",
    "{location} çocuklarla birlikteyiz, {need} {verb} {filler} {hashtags}",
    "{location} yaşlı yakınlarımız var, {need} {verb} {filler} {hashtags}",
    "{location} hasta yakınım var, {need} {verb} {filler} {hashtags}",
    "{location} kimse gelmiyor, {need} {verb} {filler} {hashtags}",
    "{location} tamamen yalnızız, {need} {verb} {filler} {hashtags}",
    "{location} depremi yaşadık, şimdi {need} {verb} {filler} {hashtags}",
    "{location} evimiz yıkıldı, {need} {verb} {filler} {hashtags}",
    "{location} dışarıda kaldık, {need} {verb} {filler} {hashtags}",
    "{location} çadırda yaşıyoruz, {need} {verb} {filler} {hashtags}",
    "{location} araçta kalıyoruz, {need} {verb} {filler} {hashtags}",
    "{location} açık havada kaldık, {need} {verb} {filler} {hashtags}",
    "{location} soğuk altında kaldık, {need} {verb} {filler} {hashtags}",
    "{location} yağmur altında kaldık, {need} {verb} {filler} {hashtags}",
    "{location} 5 kişilik aile için {need} {verb} {filler} {hashtags}",
    "{location} 10 kişilik grup için {need} {verb} {filler} {hashtags}",
    "{location} 20 kişilik ekip için {need} {verb} {filler} {hashtags}",
    "{location} 50 kişilik mahalle için {need} {verb} {filler} {hashtags}",
    "{location} 100 kişilik bölge için {need} {verb} {filler} {hashtags}",
    "{location} 3 günlük {need} {verb} {filler} {hashtags}",
    "{location} 1 haftalık {need} {verb} {filler} {hashtags}",
    "{location} günlük {need} {verb} {filler} {hashtags}",
    "{location} komşularımızla beraber {need} {verb} {filler} {hashtags}",
    "{location} mahallece {need} {verb} {filler} {hashtags}",
    "{location} toplu olarak {need} {verb} {filler} {hashtags}",
    "{location} hep birlikte {need} {verb} {filler} {hashtags}",
    "{location} gönüllülerle {need} {verb} {filler} {hashtags}",
    "{location} yetkililerden {need} {verb} {filler} {hashtags}",
    "{location} devletten {need} {verb} {filler} {hashtags}",
    "{location} belediyeden {need} {verb} {filler} {hashtags}",
    "{location} çok zor durumdayız, {need} {verb} {filler} {hashtags}",
    "{location} umutsuzluk içindeyiz, {need} {verb} {filler} {hashtags}",
    "{location} çaresizlik yaşıyoruz, {need} {verb} {filler} {hashtags}",
    "{location} umudunuzu kaybettik, {need} {verb} {filler} {hashtags}",
    "{location} moral bozukluğu içindeyiz, {need} {verb} {filler} {hashtags}",
    "{location} güç durumda kaldık, {need} {verb} {filler} {hashtags}",
    "{location} elimizden bir şey gelmiyor, {need} {verb} {filler} {hashtags}",
    "{location} sabahtan beri {need} {verb} {filler} {hashtags}",
    "{location} dünden beri {need} {verb} {filler} {hashtags}",
    "{location} 3 gündür {need} {verb} {filler} {hashtags}",
    "{location} 1 haftadır {need} {verb} {filler} {hashtags}",
    "{location} depremi sonrası {need} {verb} {filler} {hashtags}",
    "{location} felaket sonrası {need} {verb} {filler} {hashtags}",
    "{location} bu sabah {need} {verb} {filler} {hashtags}",
    "{location} bu akşam {need} {verb} {filler} {hashtags}",
    "{location} {need} {verb} acilen, {filler} {hashtags}",
    "{location} {need} {verb} hemen, {filler} {hashtags}",
    "{location} {need} {verb} bir an önce, {filler} {hashtags}",
    "{location} {need} {verb} ivedilikle, {filler} {hashtags}",
    "{location} {need} {verb} derhal, {filler} {hashtags}",
    "{location} {need} {verb} şu anda, {filler} {hashtags}",
    "{location} {need} {verb} şimdi, {filler} {hashtags}",
    "{location} {need} {verb} hala, {filler} {hashtags}",
    "{location} konumunda {need} {verb} {filler} {hashtags}",
    "{location} merkezi {need} {verb} {filler} {hashtags}",
    "{location} çevresi {need} {verb} {filler} {hashtags}",
    "{location} yakınları {need} {verb} {filler} {hashtags}",
    "{location} sınırları {need} {verb} {filler} {hashtags}",
    "{location} içerisinde {need} {verb} {filler} {hashtags}",
    "{location} etrafında {need} {verb} {filler} {hashtags}",
    "{location} dolaylarında {need} {verb} {filler} {hashtags}"
]

filler_words= [
    # Basit aciliyet
    "acil", "lütfen", "hala", "maalesef", "şu anda", "bir an önce",
    "çok acil", "en kısa sürede", "çok önemli", "öncelikli", "acil olarak",
    
    # Yardım çağrıları
    "yardım edin", "destek olun", "el uzatın", "katkı sağlayın", "paylaşın",
    "duyurun", "yaygınlaştırın", "haber verin", "bildirin", "duyarlı olun",
    
    # Durumsal
    "hala ulaşamadık", "hala bekleniyor", "hala yok", "acil ihtiyaç", "acil destek",
    "yardım bekleniyor", "yardım gerekli", "yardım lazım", "acil yardım",
    "hala eksik", "hala gönderilmedi", "hala gelmedi", "hala bulunamadı", "hala ulaştırılamadı",
    
    # Talep ifadeleri
    "acil talep", "acil gönderim", "acil çözüm", "acil yardım çağrısı", "acil destek bekleniyor",
    "ivedi olarak", "derhal", "şimdi", "hemen", "beklemeden", "gecikmeden",
    
    # Duygusal durumlar
    "çok zor durumda", "son derece acil", "can sıkıntısı", "yaşam tehlikesi",
    "çaresizlik içinde", "umutla bekliyoruz", "endişe içindeyiz", "kaygı duyuyoruz",
    
    # Aile durumları
    "ailecek mahsur kaldık", "çocuklar aç", "yaşlılar hasta", "bebekler hasta",
    "hamile eş var", "engelli yakın var", "tek başımıza", "yalnız kaldık",
    
    # İletişim durumları
    "kimse gelmiyor", "hiç yardım gelmedi", "tamamen yalnızız", "ulaşamıyoruz",
    "bağlantı kuramıyoruz", "haberleşemiyoruz", "temas kuramıyoruz",
    
    # Mekânsal durumlar
    "dışarıda kaldık", "soğukta kaldık", "yağmurda kaldık", "açık havada",
    "çadırda yaşıyoruz", "araçta kalıyoruz", "sokakta kaldık", "barınaksızız",
    
    # Sağlık durumları
    "hasta yakınımız var", "ilaç bitecek", "tedavi olması gerek", "doktor lazım",
    "hastane lazım", "ambulans lazım", "sağlık ekibi lazım",
    
    # Zaman ifadeleri
    "günlerdir", "saatlerdir", "haftalardır", "aylardır", "depremi sonrası",
    "felaket sonrası", "sabahtan beri", "dünden beri", "uzun süredir",
    
    # Miktarsal
    "çok az kaldı", "neredeyse bitti", "son damlası", "son parçası",
    "bir avuç", "birkaç tane", "sayılı adet", "kısıtlı miktar",
    
    # Sosyal
    "komşularla beraber", "mahallece", "hep birlikte", "toplu olarak",
    "organize şekilde", "sistematik olarak", "koordineli şekilde"
]

sample_verbs = [
    # Temel ihtiyaç fiilleri
    "lazım", "gerekiyor", "ihtiyaç var", "şart", "zorunlu", "elzem",
    
    # Acil durumlar
    "acil lazım", "acil gerekiyor", "acil ihtiyaç var", "an acil", "çok acil", "son derece acil",
    "acil isteniyor", "acil bekleniyor", "acil talep ediliyor", "acil gönderilmeli", "acil ulaştırılmalı",
    "acil sağlanmalı", "acil temin edilmeli", "acil eksik", "acil yok", "acil bulunamıyor",
    
    # Talep ve rica
    "talep ediliyor", "rica ediliyor", "isteniyor", "arzu ediliyor", "dileniyor",
    "bekleniyor", "ümit ediliyor", "umut ediliyor",
    
    # Eksiklik ifadeleri
    "yok", "eksik", "bulunamıyor", "mevcut değil", "elimizde yok", "hiç yok",
    "hala yok", "hala eksik", "hala bulunamıyor", "hala gelmedi", "hala ulaşmadı",
    "hala gönderilmedi", "hala bulunamadı", "hala ulaştırılamadı", "hala temin edilemedi",
    
    # Gönderim ve ulaştırma
    "gönderilmeli", "ulaştırılmalı", "teslim edilmeli", "iletilmeli", "aktarılmalı",
    "sağlanmalı", "temin edilmeli", "karşılanmalı", "getirililmeli", "taşınmalı",
    
    # Bekleme durumları
    "bekleniyor", "beklemede", "bekliyoruz", "sabırsızlıkla bekleniyor", "merakla bekleniyor",
    "hala bekleniyor", "hala beklemede", "günlerdir bekleniyor", "saatlerdir bekleniyor",
    
    # Bulma ve edinme
    "bulunmalı", "bulunması gerek", "edinilmeli", "alınmalı", "satın alınmalı",
    "toplanmalı", "hazırlanmalı", "organize edilmeli",
    
    # Yardım talepleri
    "yardım ediliyor", "destek bekleniyor", "katkı sağlanıyor", "fedakarlık yapılıyor",
    "gönüllülük yapılıyor", "dayanışma gösteriliyor", "el uzatılıyor",
    
    # Durumsal ifadeler
    "tükendi", "bitti", "kalmadı", "azaldı", "yetersiz", "kifayetsiz", "az geldi",
    "yetmiyor", "doymuyor", "karşılamıyor", "gidermiyor"
]

sample_needs = [
    # TEMEL GIDA İHTİYAÇLARI (35)
    "su", "ekmek", "pirinç", "makarna", "bulgur", "mercimek", "nohut", "fasulye",
    "konserve et", "konserve balık", "konserve sebze", "kuru meyve", "bisküvi",
    "çay", "kahve", "şeker", "tuz", "yağ", "un", "pekmez", "reçel", "bal",
    "süt", "ayran", "meyve suyu", "kraker", "gofret", "çikolata", "kek",
    "yumurta", "peynir", "zeytin", "turşu", "salça", "baharat",
    
    # BEBEK VE ÇOCUK İHTİYAÇLARI (25)
    "bebek maması", "bebek bezi", "islak mendil", "biberon", "emzik", "bebek pudrası",
    "bebek şampuanı", "bebek kremi", "çocuk vitamini", "çocuk ilacı", "oyuncak",
    "boyama kitabı", "kalem", "defter", "çocuk kıyafeti", "çocuk ayakkabısı",
    "çocuk montu", "çocuk battaniyesi", "çocuk yastığı", "mama sandalyesi",
    "çocuk bezi", "çocuk mendili", "çocuk losyonu", "çocuk sabunu", "çocuk şampuanı",
    
    # BARINMA VE UYKU (20)
    "çadır", "battaniye", "yorgan", "yastık", "uyku tulumu", "minder", "yatak",
    "branda", "naylon örtü", "sünger yatak", "kamp sandalyesi", "kamp masası",
    "hasır", "kilim", "halı", "kamp yatağı", "şişme yatak", "uyku minderi",
    "çadır direği", "çadır örtüsü",
    
    # HİJYEN VE TEMİZLİK (25)
    "sabun", "şampuan", "diş macunu", "diş fırçası", "tuvalet kağıdı", "havlu",
    "kadın hijyen ürünü", "tıraş bıçağı", "tıraş köpüğü", "deodorant", "parfüm",
    "el kremi", "yüz kremi", "kağıt havlu", "temizlik bezi", "deterjan",
    "çamaşır deterjanı", "bulaşık deterjanı", "dezenfektan", "eldiven",
    "kolonya", "antiseptik jel", "ıslak mendil", "kuru mendil", "makyaj malzemesi",
    
    # KIYAFET VE AYAKKABI (22)
    "mont", "kaban", "kazak", "sweatshirt", "tişört", "pantolon", "eşofman",
    "iç çamaşırı", "çorap", "ayakkabı", "bot", "terlik", "eldiven", "atkı",
    "bere", "şapka", "yağmurluk", "kemer", "şort", "etek", "bluz", "gömlek",
    
    # SAĞLIK VE İLK YARDIM (30)
    "ilaç", "ağrı kesici", "ateş düşürücü", "antibiyotik", "vitamin", "sargı bezi",
    "yara bandı", "antiseptik", "merhem", "göz damlası", "kulak damlası",
    "tansiyon ilacı", "şeker ilacı", "kalp ilacı", "astım ilacı", "maske",
    "termometre", "serum", "enjektör", "pamuk", "gazlı bez", "ilk yardım çantası",
    "aspirin", "parol", "voltaren", "betadin", "rifocin", "flaster", "buz torbası", "soğuk kompres",
    
    # TEKNOLOJİ VE İLETİŞİM (15)
    "telefon", "şarj aleti", "powerbank", "pil", "el feneri", "radyo",
    "internet", "wifi", "laptop", "tablet", "kablo", "adaptör",
    "solar panel", "jeneratör", "akü",
    
    # MUTFAK VE YEMEKLİK (20)
    "tencere", "tava", "çatal", "kaşık", "bıçak", "tabak", "kase", "bardak",
    "çay bardağı", "termos", "çaydanlık", "ocak", "tüp", "kibrit", "çakmak",
    "açacak", "doğrayıcı", "süzgeç", "kevgir", "spatula",
    
    # ISITMA VE ENERJİ (12)
    "soba", "odun", "kömür", "gaz", "mum", "gazyağı", "jeneratör", "akü",
    "güneş paneli", "ısıtıcı", "elektrik sobası", "kamp sobası",
    
    # NAKLİYE VE ULAŞIM (10)
    "araç", "yakıt", "benzin", "motorin", "lastik", "yedek parça", "bisiklet", "akü",
    "motor yağı", "antifriz",
    
    # YAŞLI VE HASTA İHTİYAÇLARI (12)
    "tekerlekli sandalye", "baston", "yürüteç", "hasta yatağı", "oksijen tüpü",
    "nebülizatör", "tansiyon aleti", "şeker ölçer", "hasta bezi", "hasta bakım ürünü",
    "serum askısı", "hasta minderi",
    
    # HAYVAN İHTİYAÇLARI (8)
    "köpek maması", "kedi maması", "kuş yemi", "hayvan tasması", "hayvan kafesi", 
    "veteriner ihtiyacı", "hayvan ilacı", "hayvan yemi",
    
    # TAMİR VE İNŞAAT (12)
    "çivi", "vida", "çekiç", "tornavida", "testere", "ip", "tel", "bant", 
    "yapıştırıcı", "matkap", "rende", "pense",
    
    # DİĞER ACİL İHTİYAÇLAR (14)
    "çöp poşeti", "plastik kova", "su bidonu", "jerry can", "teneke", "karton kutu",
    "naylon poşet", "streç film", "alüminyum folyo", "kağıt tabak", "plastik tabak",
    "çanta", "valiz", "torba"
]

sample_needs_categorized = {
    "gıda": [
        "su", "ekmek", "pirinç", "makarna", "bulgur", "mercimek", "nohut", "fasulye",
        "konserve et", "konserve balık", "konserve sebze", "kuru meyve", "bisküvi",
        "çay", "kahve", "şeker", "tuz", "yağ", "un", "pekmez", "reçel", "bal",
        "süt", "ayran", "meyve suyu", "kraker", "gofret", "çikolata", "kek",
        "yumurta", "peynir", "zeytin", "turşu", "salça", "baharat",
    ],
    "bebek/çocuk": [
        "bebek maması", "bebek bezi", "islak mendil", "biberon", "emzik", "bebek pudrası",
        "bebek şampuanı", "bebek kremi", "çocuk vitamini", "çocuk ilacı", "oyuncak",
        "boyama kitabı", "kalem", "defter", "çocuk kıyafeti", "çocuk ayakkabısı",
        "çocuk montu", "çocuk battaniyesi", "çocuk yastığı", "mama sandalyesi",
        "çocuk bezi", "çocuk mendili", "çocuk losyonu", "çocuk sabunu", "çocuk şampuanı",
    ],
    "barınma/uyku": [
        "çadır", "battaniye", "yorgan", "yastık", "uyku tulumu", "minder", "yatak",
        "branda", "naylon örtü", "sünger yatak", "kamp sandalyesi", "kamp masası",
        "hasır", "kilim", "halı", "kamp yatağı", "şişme yatak", "uyku minderi",
        "çadır direği", "çadır örtüsü",
    ],
    "hijyen/temizlik": [
        "sabun", "şampuan", "diş macunu", "diş fırçası", "tuvalet kağıdı", "havlu",
        "kadın hijyen ürünü", "tıraş bıçağı", "tıraş köpüğü", "deodorant", "parfüm",
        "el kremi", "yüz kremi", "kağıt havlu", "temizlik bezi", "deterjan",
        "çamaşır deterjanı", "bulaşık deterjanı", "dezenfektan", "eldiven",
        "kolonya", "antiseptik jel", "ıslak mendil", "kuru mendil", "makyaj malzemesi",
    ],
    "kıyafet/ayakkabı": [
        "mont", "kaban", "kazak", "sweatshirt", "tişört", "pantolon", "eşofman",
        "iç çamaşırı", "çorap", "ayakkabı", "bot", "terlik", "eldiven", "atkı",
        "bere", "şapka", "yağmurluk", "kemer", "şort", "etek", "bluz", "gömlek",
    ],
    "sağlık/ilk yardım": [
        "ilaç", "ağrı kesici", "ateş düşürücü", "antibiyotik", "vitamin", "sargı bezi",
        "yara bandı", "antiseptik", "merhem", "göz damlası", "kulak damlası",
        "tansiyon ilacı", "şeker ilacı", "kalp ilacı", "astım ilacı", "maske",
        "termometre", "serum", "enjektör", "pamuk", "gazlı bez", "ilk yardım çantası",
        "aspirin", "parol", "voltaren", "betadin", "rifocin", "flaster", "buz torbası", "soğuk kompres",
    ],
    "teknoloji/iletisim": [
        "telefon", "şarj aleti", "powerbank", "pil", "el feneri", "radyo",
        "internet", "wifi", "laptop", "tablet", "kablo", "adaptör",
        "solar panel", "jeneratör", "akü",
    ],
    "mutfak/yemek": [
        "tencere", "tava", "çatal", "kaşık", "bıçak", "tabak", "kase", "bardak",
        "çay bardağı", "termos", "çaydanlık", "ocak", "tüp", "kibrit", "çakmak",
        "açacak", "doğrayıcı", "süzgeç", "kevgir", "spatula",
    ],
    "ısıtma/enerji": [
        "soba", "odun", "kömür", "gaz", "mum", "gazyağı", "jeneratör", "akü",
        "güneş paneli", "ısıtıcı", "elektrik sobası", "kamp sobası",
    ],
    "nakliye/ulaşım": [
        "araç", "yakıt", "benzin", "motorin", "lastik", "yedek parça", "bisiklet", "akü",
        "motor yağı", "antifriz",
    ],
    "yaşlı/hasta": [
        "tekerlekli sandalye", "baston", "yürüteç", "hasta yatağı", "oksijen tüpü",
        "nebülizatör", "tansiyon aleti", "şeker ölçer", "hasta bezi", "hasta bakım ürünü",
        "serum askısı", "hasta minderi",
    ],
    "hayvan": [
        "köpek maması", "kedi maması", "kuş yemi", "hayvan tasması", "hayvan kafesi", 
        "veteriner ihtiyacı", "hayvan ilacı", "hayvan yemi",
    ],
    "tamir/inşaat": [
        "çivi", "vida", "çekiç", "tornavida", "testere", "ip", "tel", "bant", 
        "yapıştırıcı", "matkap", "rende", "pense",
    ],
    "diğer": [
        "çöp poşeti", "plastik kova", "su bidonu", "jerry can", "teneke", "karton kutu",
        "naylon poşet", "streç film", "alüminyum folyo", "kağıt tabak", "plastik tabak",
        "çanta", "valiz", "torba"
    ],
}

sample_hashtags = [
    "#deprem", "#yardım", "#enkaz", "#acil", "#acilyardım", "#afad", "#afet", "#ihtiyaç",
    "#gönüllü", "#arama_kurtarma", "#umutol", "#geçmişolsun", "#dayanışma", "#birlik",
    "#destek", "#yardımlaşma", "#felaket", "#kriz", "#acildurum", "#sosyardım",
    "#hayırseverlik", "#kampanya", "#bağış", "#kurtarma", "#insanlık"
]

sample_users = [f"user_{i}" for i in range(7500)]

sample_locations = [
    # İstanbul
    "istanbul", "istanbul avcılar", "istanbul kadıköy", "istanbul beşiktaş", "istanbul pendik", "istanbul kartal", "istanbul maltepe", "istanbul üsküdar", "istanbul şişli", "istanbul fatih", "istanbul bakırköy", "istanbul ataşehir", "istanbul bahçelievler", "istanbul büyükçekmece", "istanbul sultanbeyli", "istanbul beylikdüzü",
    # İzmir
    "izmir", "izmir bornova", "izmir buca", "izmir karşıyaka", "izmir konak", "izmir bayraklı", "izmir balçova", "izmir gaziemir", "izmir seferihisar", "izmir menemen", "izmir alsancak",
    # Kocaeli
    "kocaeli", "kocaeli izmit", "kocaeli gebze", "kocaeli gölcük", "kocaeli darıca", "kocaeli körfez", "kocaeli derince", "kocaeli değirmendere",
    # Sakarya
    "sakarya", "sakarya adapazarı", "sakarya arifiye", "sakarya serdivan", "sakarya hendek", "sakarya akyazı",
    # Bursa
    "bursa", "bursa nilüfer", "bursa osmangazi", "bursa yıldırım", "bursa gemlik", "bursa mudanya",
    # Manisa
    "manisa", "manisa akhisar", "manisa salihli", "manisa turgutlu", "manisa soma",
    # Balıkesir
    "balıkesir", "balıkesir bandırma", "balıkesir edremit", "balıkesir altıeylül",
    # Çanakkale
    "çanakkale", "çanakkale ayvacık", "çanakkale ezine", "çanakkale biga",
    # Hatay
    "hatay", "hatay merkez", "hatay antakya", "hatay defne", "hatay samandağ", "hatay iskenderun", "hatay kırıkhan",
    # Adana
    "adana", "adana seyhan", "adana yüreğir", "adana çukurova", "adana ceyhan",
    # Osmaniye
    "osmaniye", "osmaniye merkez", "osmaniye kadirli",
    # Malatya
    "malatya", "malatya battalgazi", "malatya yeşilyurt", "malatya doğanyol",
    # Elazığ
    "elazığ", "elazığ sivrice", "elazığ palu",
    # Erzincan
    "erzincan", "erzincan kemaliye", "erzincan refahiye",
    # Düzce
    "düzce", "düzce akçakoca",
    # Van
    "van", "van erciş", "van tuşba",
    # Tekirdağ
    "tekirdağ", "tekirdağ çorlu", "tekirdağ süleymanpaşa",
    # Aydın
    "aydın", "aydın nazilli", "aydın söke", "aydın kuşadası",
    # Muğla
    "muğla", "muğla bodrum", "muğla fethiye", "muğla marmaris",
    # Antalya
    "antalya", "antalya alanya", "antalya manavgat", "antalya kaş",
    # Denizli
    "denizli", "denizli pamukkale", "denizli merkezefendi", "denizli acıpayam"
]

sample_created_at = [(datetime.datetime(2025, 8, 5, 5, 0, 0) + datetime.timedelta(minutes=random.randint(0, 10080))) for _ in range(1500)]

def join_with_mixed_operators(item_list: list[str]) -> str:
    result = item_list[0] if len(item_list) > 0 else ""
    separators = [" ve ", ", ", " ", " + ", " & "]

    for i in range(1, len(item_list)):
        result += random.choice(separators) + item_list[i]
    
    return result 

def generate_mock_data(quantity: int, table: boto3.resource("dynamodb").Table):
    for i in range(quantity):
        template = random.choice(templates)
        needs = join_with_mixed_operators(random.sample(sample_needs, random.randint(1, 3)))
        verb = random.choice(sample_verbs)
        hashtags = random.sample(sample_hashtags, random.randint(1, 3))
        hashtags_joined = " ".join(hashtags)
        fillers = join_with_mixed_operators(random.sample(filler_words, random.randint(0, 2)))
        location = random.choice(sample_locations)
        created_at = random.choice(sample_created_at)
        user = random.choice(sample_users)
        text = template.format(
            location=location,
            need=needs,
            filler=fillers,
            verb=verb,
            hashtags=hashtags_joined
        )
        tweet_id = str(1620000000001100000 + i)

        try:
            table.put_item(Item={
                "tweet_id": tweet_id,
                "text": text,
                "created_at": str(created_at), # DynamoDB DOES NOT support datetime objects, convert to string before adding to the table
                "user": user,
                "hashtags": hashtags
            })
        except Exception as e:
            print(f"Exception occured while adding tweet {tweet_id}: {e}")
            continue
        
        if i % 100 == 0:
            print(f"{i} tweets added, %{100 * i / quantity} done.")

    print("done")

generate_mock_data(2500, mock_data_table) # UNCOMMENT THIS TO GENERATE THE MOCK DATA

def get_need_category(need:str) -> str:
    if need == "":
        return ""
    
    for category, needs in sample_needs_categorized.items():
        if need in needs:
            return category
    return "diğer"

def select_categorized_needs():
    needs = []
    categories = []

    for i in range(random.randint(1, 3)):
        categories.append(random.choice(list(sample_needs_categorized.keys())))
        needs.append(random.choice(sample_needs_categorized[categories[-1]]))

    return needs, categories

def generate_categorized_data(quantity: int, table: boto3.resource("dynamodb").Table):
    for i in range(quantity):
        needs, categories = select_categorized_needs()
        needs_text = join_with_mixed_operators(needs)
        template = random.choice(templates)
        verb = random.choice(sample_verbs)
        hashtags = random.sample(sample_hashtags, random.randint(1, 3))
        hashtags_joined = " ".join(hashtags)
        fillers = join_with_mixed_operators(random.sample(filler_words, random.randint(0, 2)))
        location = random.choice(sample_locations)
        created_at = random.choice(sample_created_at)
        user = random.choice(sample_users)
        text = template.format(
            location=location,
            need=needs_text,
            filler=fillers,
            verb=verb,
            hashtags=hashtags_joined
        )
        labels = {
            "needs": needs,
            "categories": categories,
            "unique_categories": list(set(categories)),
            "location": location,
            "has_multiple_categories": len(set(categories)) > 1,
            "category_count": len(set(categories)),
            "verb": verb,
            "fillers": fillers,
            "hashtags": hashtags,
        }
        tweet_id = str(1620000010000000000 + i)

        try:
            table.put_item(Item={
                "tweet_id": tweet_id,
                "text": text,
                "created_at": str(created_at),
                "user": user,
                "hashtags": hashtags,
                "is_labeled": True,
                "labels": labels
            })
        except Exception as e:
            print(f"Exception occured while adding tweet {tweet_id}: {e}")
            continue

        if i % 100 == 0:
            print(f"{i} tweets added, %{100 * i / quantity} done.")
    
    print("done")

generate_categorized_data(10000, mock_data_table) # UNCOMMENT THIS TO GENERATE CATEGORIZED MOCK DATA