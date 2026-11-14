# Application Fire TV / Android TV

`soen-firetv.apk` (à la racine du dépôt) contient le jeu complet : il marche sans internet, et l'application
transmet au jeu toutes les touches de la manette (Xbox, PlayStation…) et de la télécommande.

## Installer sur un Fire TV Stick
1. Paramètres → Ma Fire TV → À propos : cliquer 7 fois sur le nom de l'appareil pour afficher les Options pour les développeurs.
2. Ma Fire TV → Options pour les développeurs → Installer des applis inconnues → autoriser **Downloader**.
3. Installer l'application **Downloader** (gratuite, dans la boutique Amazon) et y taper l'adresse :
   `https://github.com/nezzirf/latour/raw/main/soen-firetv.apk`
4. Installer, puis lancer **Soen : la Tour** depuis « Vos applis ».

## Fabriquer l'application
`VCODE=3 VNAME=1.2 ./build.sh` (augmenter VCODE à chaque version) assemble `index.html` (CSS Tailwind compilé et three.js intégrés), le code `src/`, et signe le
paquet avec `soen.keystore` (garder la même clé pour que les mises à jour s'installent par-dessus sans perdre les os et achats).
Il faut le SDK Android (plateforme 34, build-tools 34.0.0), Java, Node (tailwindcss) et three@0.147.0.
