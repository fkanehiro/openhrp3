ruby collect.rb
ruby create_wxs.rb
candle tvmet.wxs -out tvmet.wixobj
light -ext WixUIExtension -out tvmet-1.7.2-win.msi tvmet.wixobj