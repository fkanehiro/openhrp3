ruby collect.rb
ruby create_wxs.rb
candle Eigen.wxs -out Eigen.wixobj
light -ext WixUIExtension -out eigen-3.0.2-win.msi Eigen.wixobj