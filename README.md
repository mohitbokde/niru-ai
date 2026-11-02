# NIRU AI (Android, Hindi)

Ek dost jaisi Hindi AI, jiske paas 2D animated character hai: **निया** (लड़की) ya **रॉकी** (लड़का).
Chehra aapke shabdon ke hisaab se badalta hai (khushi, udasi, gussa, chinta, hairani).
VOICE MODE ON/OFF ek bade switch se hota hai.

## Kya-kya kaam karta hai

- हिंदी/हिंग्लिश chat, dost jaisa tone
- Character chunna: निया ya रॉकी
- 2D character: palak jhapakna, saans lena, bolte waqt mooh chalna, chehre ke bhaav
- Shabdon se mood ka andaza, aur uske hisaab se chehra
- VOICE MODE OFF: kuch nahi bolegi, mic bhi nahi sunega (sirf aapke tap par)
- VOICE MODE ON: jawab bolegi, notification me Rokein / Band karein buttons, app background me bhi bol sakti hai
- Gemini key dalne par asli AI jawab; key na ho to offline simple jawab

## Seema (zaroor padhein)

- Camera se chehra nahi padhta. Mood sirf likhe hue shabdon se pata chalta hai, isliye galat bhi ho sakta hai.
- Background me mic nahi sunta. Android 12+ par background se mic chalana mana hai.
- Hindi awaaz tabhi bolegi jab phone me Google Text-to-Speech ki Hindi voice installed ho.
- Gemini ki free quota limited hoti hai.
- API key phone me sirf is app ke andar save hoti hai. Public app me key daalna surakshit nahi hai; production ke liye backend chahiye.
- 2D character ek simple vector drawing hai, jo Canvas se bana hai. Ye Live2D jaisi detailed animation nahi hai.

## Build kaise karein (phone se, GitHub Actions ke zariye)

Termux me APK banana bhari hota hai. Isliye code GitHub par daalein, wahan APK apne aap banegi.

### Step 1: GitHub par repo banayein
1. github.com par account banayein (free).
2. New repository banayein, naam: `niru-ai`.

### Step 2: Termux se code bhejein
Termux F-Droid se install karein, phir:

```bash
pkg update && pkg install git unzip
termux-setup-storage
cd ~/storage/downloads
unzip NIRU_AI.zip
cd NIRU_AI
git init
git add .
git commit -m "NIRU AI first version"
git branch -M main
git remote add origin https://github.com/APNA_USERNAME/niru-ai.git
git push -u origin main
```

Push ke time username aur password maangega. Password ki jagah GitHub ka **Personal Access Token** dalein (GitHub > Settings > Developer settings > Tokens, `repo` permission ke saath).

Note: `.github` folder hidden hai, zip me ye included hai. Ise `ls -la` se check karein.

### Step 3: APK banwayein
1. GitHub repo me **Actions** tab kholein.
2. **Build NIRU APK** workflow chalega (push ke baad apne aap, ya Run workflow button se).
3. Green tick aane par us run ko kholkar **NIRU-debug-apk** artifact download karein.
4. Zip khulne par `app-debug.apk` milegi.

Build fail hua to Actions ka red log mujhe bhejein, main exact fix bataunga.

### Step 4: Install aur test
1. APK install karein (Phone settings me "unknown apps" ki permission dena padega).
2. App kholein, **सेटिंग** me Gemini API key dalein (Google AI Studio: aistudio.google.com).
3. Test checklist:
   - [ ] निया/रॉकी chip badalne par character badle
   - [ ] "मैं बहुत उदास हूँ" likhne par chehra udaas ho
   - [ ] "आज बहुत मज़ा आया!" par chehra khush ho
   - [ ] VOICE MODE OFF: koi awaaz nahi aati
   - [ ] VOICE MODE ON: notification aati hai, jawab bolti hai
   - [ ] Rokein button awaaz rokta hai, Band karein voice mode band karta hai
   - [ ] 🎤 tap karke Hindi bolkar message bhejna

## Project structure

```
NIRU_AI/
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── app/build.gradle.kts
├── app/src/main/AndroidManifest.xml
├── app/src/main/java/com/niru/ai/
│   ├── MainActivity.kt     (entry)
│   ├── NiruScreen.kt       (chat, switch, settings UI)
│   ├── Character.kt        (2D animated character)
│   ├── Brain.kt            (Gemini + offline jawab + persona)
│   ├── Emotion.kt          (shabdon se mood)
│   ├── NiruVoice.kt        (bolna + sunna)
│   ├── VoiceService.kt     (foreground service, notification)
│   └── Prefs.kt            (settings save)
└── .github/workflows/android.yml   (APK build)
```

## Agle steps (abhi nahi bane)

- Calculator, YouTube kholna, web search jaise tools (Gemini function calling ke saath)
- Bolkar wake word ("NIRU") — alag design chahiye
- Flute music section aur practice
- Live2D jaisa detailed character, ya camera-based chehra pehchan (sahmati aur privacy ke saath)
