#!/bin/sh
#
# @file pkg_install_ubuntu.sh
# @brief  preprocessing script of OpenHRP3 dependent packages install for Ubuntu
# @author Keisuke Saeki
#         Noriaki Ando
#         Shinji Kurihara
#         Tetsuo Ando
#         Harumi Miyamoto
#

#---------------------------------------
# ロケールの言語確認
#---------------------------------------
check_lang()
{
lang="en"

locale | grep ja_JP > /dev/null && lang="jp"

if test "$lang" = "jp" ;then
    msg1="ディストリビューションを確認してください。\nDebianかUbuntu以外のOSの可能性があります。"
    msg2="コードネーム :"
    msg3="このOSはサポートしておりません。"
    msg4="OpenRTM-aist のリポジトリが登録されていません。"
    msg5="Source.list に OpenRTM-aist のリポジトリ: "
    msg6="を追加します。よろしいですか？(y/n)[y] "
    msg7="中断します。"
    msg8="ルートユーザーで実行してください。"
    msg9="インストール中です..."
    msg10="完了"
    msg11="アンインストール中です."
    msg12="Canonical Partner のリポジトリが登録されていません。"
    msg13="Source.list に Canonical Partner Repository のリポジトリ: "
else
    msg1="This distribution may not be debian/ubuntu."
    msg2="The code name is : "
    msg3="This OS is not supported."
    msg4="No repository entry for OpenRTM-aist is configured in your system."
    msg5="repository entry for OpenrRTM-aist: "
    msg6="Do you want to add new repository entry for OpenrRTM-aist in source.list? (y/n) [y] "
    msg7="Abort."
    msg8="This script should be run as root."
    msg9="Now installing: "
    msg10="done."
    msg11="Now uninstalling: "
    msg12="No repository entry for the Canonical Partner is configured in your system."
    msg13="repository entry for the Canonical Partner: "
fi

}


#---------------------------------------
# リポジトリサーバ
#---------------------------------------
create_srclist () {
    codename=`sed -n /DISTRIB_CODENAME=/p /etc/lsb-release`
    cnames=`echo "$codename" | sed 's/DISTRIB_CODENAME=//'`
    #cnames="sarge edgy feisty gutsy hardy intrepid"
    for c in $cnames; do
	if test -f "/etc/apt/sources.list"; then
	    res=`grep $c /etc/apt/sources.list`
	else
	    echo $msg1
	    exit
	fi
	if test ! "x$res" = "x" ; then
	    code_name=$c
	fi
    done
    if test ! "x$code_name" = "x"; then
	echo $msg2 $code_name
    else
	echo $msg3
	exit
    fi
    openrtm_repo="deb http://www.openrtm.org/pub/Linux/ubuntu/ $code_name main"
}

#---------------------------------------
# ソースリスト更新関数の定義
#---------------------------------------
update_source_list () {
    rtmsite=`grep openrtm /etc/apt/sources.list`
    if test "x$rtmsite" = "x" ; then
	echo $msg4
	echo $msg5
	echo "  " $openrtm_repo
	read -p $msg6 kick_shell

	if test "x$kick_shell" = "xn" ; then
	    echo $msg7
	else
	    echo $openrtm_repo >> /etc/apt/sources.list
	fi
    fi
}

#---------------------------------------
# ソースリスト更新関数の定義
# sun-java6-* インストールのため(10.04以降)
#---------------------------------------
update_source_list_partner () {
    partner_repo="deb http://archive.canonical.com/ubuntu $code_name partner"
    if test "$code_name" = "natty" ; then
        partnersite=`grep "^deb http://archive.canonical.com/.* $code_name partner$" /etc/apt/sources.list`
        if test "x$partnersite" = "x" ; then
            echo $msg12
            echo $msg13
            echo "  " $partner_repo
            read -p $msg6 kick_shell

            if test "x$kick_shell" != "xn" ; then
                add-apt-repository "$partner_repo"
            fi
        fi
    fi
}

#----------------------------------------
# root かどうかをチェック
#----------------------------------------
check_root () {
    if test ! `id -u` = 0 ; then
	echo ""
	echo $msg8
	echo $msg7
	echo ""
	exit 1
    fi
}

#---------------------------------------
# メイン
#---------------------------------------
check_lang
check_root
create_srclist
update_source_list
update_source_list_partner
apt-get update
