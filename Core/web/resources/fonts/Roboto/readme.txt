Downloaded this CSS file and ran regex on it, downloaded all woff files in DownloadThemAll firefox plugin
http://fonts.googleapis.com/css?family=Roboto:300italic,400italic,600italic,700italic,800italic,400,300,600,700,800

With Chrome you get woff2 format with various character sets
Search:
/\* (.*?) \*/(.*?) font-family: '(.*?)';(.*?)src: local\('(.*?)'\), local\('(.*?)'\), url\((.*?)\) format\('(.*?)'\)(.*?)}
Replace to generate new CSS:
/* \1 */\2 font-family: '\3';\4src: local('\5'), local('\6'), url(/resources/fonts/\3/\6-\1.\8) format('\8')\9}
Replace to get HTML links:
<a href="\7">\6-\1.\8</a><br/>

With Firefox you get woff format instead of woff2, you only get one character set, latin in my case
Search:
@font-face {
  font-family: '(.*?)';
  font-style: (.*?);
  font-weight: (.*?);
  src: local\('(.*?)'\), local\('(.*?)'\), url\((.*?)\) format\('(.*?)'\), url\((.*?)\) format\('(.*?)'\);
}
Replace to generate new CSS:
@font-face {
  font-family: '\1';
  font-style: \2;
  font-weight: \3;
  src: local('\4'), local('\5'), url(/resources/fonts/\1/\5-latin.\7) format('\7'), url(/resources/fonts/\1/\5-latin.\9) format('\9');
}
Replace to get HTML links:
<a href="\8">\5-latin.\9</a><br/>