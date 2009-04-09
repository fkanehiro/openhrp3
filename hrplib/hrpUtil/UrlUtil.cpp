/*! @file
  @author Takafumi.Tawara
*/

#include "UrlUtil.h"

#if (BOOST_VERSION <= 103301)
#include <boost/filesystem/path.hpp>
#include <boost/filesystem/operations.hpp>
#else
#include <boost/filesystem.hpp>
#endif

using namespace boost;


/*!  
  @if jp
  @brief URLスキーム(file:)文字列を削除  
  @param[in]    url 検索・置換対象となるURL  
  @return string URLスキーム文字列を取り除いた文字列  
  @endif
*/
string hrp::deleteURLScheme(string url)
{
    static const string fileProtocolHeader1("file://");
    static const string fileProtocolHeader2("file:");

    size_t pos = url.find( fileProtocolHeader1 );
    if( 0 == pos ) {
        url.erase( 0, fileProtocolHeader1.size() );
    } else {
        size_t pos = url.find( fileProtocolHeader2 );
        if( 0 == pos ) {
            url.erase( 0, fileProtocolHeader2.size() );
        }
    }

    // Windows ドライブ文字列の時はディレクトリ区切り文字分をさらに削除//
    if ( url.find(":") == 2 && url.find("/") ==0 )
        url.erase ( 0, 1 );

    return url;
}


/*!  
  @if jp
  @brief URLスキーム文字列を取り除いた文字列を生成する  
  @param[out]   refUrl  URLスキーム文字列を取り除いた文字列を格納  
  @param[in]    rootDir 親ディレクトリ     
  @param[in]    srcPath 元となるURL  
  @return URLスキーム文字列を取り除いた文字列  
  @endif
*/
void hrp::getPathFromUrl(string& refUrl, const string& rootDir, string srcUrl)
{
    if ( isFileProtocol(srcUrl) ){   // ローカルファイル //
        filesystem::path filepath( deleteURLScheme(srcUrl), filesystem::native);
        if(filesystem::exists(filepath)){    // 元が絶対パス //
            refUrl = filesystem::system_complete(filepath).file_string();
        }else{               // 元が相対パス //
            filesystem::path filepath(rootDir + deleteURLScheme(srcUrl), filesystem::native);
            if(filesystem::exists(filepath)){
                refUrl = filesystem::system_complete(filepath).file_string();
            }
        }
    } else {
    	// ファイルスキーム以外の処理 //
    }
}


/*!  
  @if jp
  @brief URLがファイルかどうかの判定  
  @param[in]    ref  判定対象のURL  
  @return boolean true:ローカルファイルである  
  @endif
*/
bool hrp::isFileProtocol(const string& ref)
{
    bool ret = false;
    string::size_type pos = ref.find(":");
    if ( pos == string::npos || pos == 1 )
    {
        // Directly local path || Windows drive letter separator
        ret = true;
    } else {
        if( ref.find("file:") == 0 )
            ret = true;
    }
    return ret;
}
