ruby collect_x64.rb
ruby create_wxs_x64.rb
candle -arch x64 -out boost.wixobj boost.wxs
light -ext WixUIExtension -out boost-1.44_x64.msi boost.wixobj