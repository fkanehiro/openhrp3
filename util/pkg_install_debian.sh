#!/bin/sh
#
# @file pkg_install_debian.sh
# @brief OpenRTM-aist dependent packages install script for Debian-sarge
# @author Noriaki Ando <n-ando@aist.go.jp>
#         Shinji Kurihara
#         Tetsuo Ando
#         Harumi Miyamoto
#         Nobu Kawauchi
#

#---------------------------------------
# パッケージリスト
#---------------------------------------
set_package_list()
{
omni="libomniorb4-dev omniidl"
ace="libace libace-dev"
openrtm="openrtm-aist openrtm-aist-doc openrtm-aist-dev openrtm-aist-example"
openrtm04="openrtm-aist=0.4.2-1 openrtm-aist-doc=0.4.2-1 openrtm-aist-dev=0.4.2-1 openrtm-aist-example=0.4.2-1"
pyyaml="python-support python-yaml"
devel="gcc g++ make uuid-dev"
packages="$devel $omni $pyyaml $openrtm"
u_packages="$omni $ace $openrtm "

default_reposerver="openrtm.org"
reposervers="openrtm.org"
reposerver=""
}

#---------------------------------------
# ロケールの言語確認
#---------------------------------------
check_lang()
{
lang="en"

locale | grep ja_JP > /dev/null && lang="jp"

if test "$lang" = "jp" ;then
    msg1="ディストリビューションを確認してください。\nDebian以外のOSの可能性があります。"
    msg2="コードネーム ： "
    msg3="このOSはサポートされておりません。"
    msg4=" OpenRTM-aistのリポジトリが登録されていません。"
    msg5="Source.listにOpenRTM-aistのリポジトリ："
    msg6="を追加します。よろしいですか？(y/n)[y] "
    msg7="中断します。"
    msg8="ルートユーザーで実行してください。"
    msg9="インストール中です..."
    msg10="完了"
    msg11="アンインストール中です"
else
    msg1="This distribution may not be debian/ubuntu."
    msg2="The code name is : "
    msg3="This OS is not supported."
    msg4="No repository entry for OpenRTM-aist is configured in your system."
    msg5="repository entry for OpenRTM-aist: "
    msg6="Do you want to add the repository entry for OpenRTM-aist in source.list?(y/n)[y] "
    msg7="Abort."
    msg8="This script should be run as root."
    msg9="Now installing: "
    msg10="done."
    msg11="Now uninstalling: "

fi

}

#---------------------------------------
# コードネーム取得
#---------------------------------------
check_codename () {
    cnames="wheezy jessie"
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
}

#----------------------------------------
# 近いリポジトリサーバを探す
#----------------------------------------
check_reposerver()
{
    minrtt=65535
    nearhost=''
    for host in $reposervers; do
	rtt=`ping -c 1 $host | grep 'time=' | sed -e 's/^.*time=\([0-9\.]*\) ms.*/\1/' 2> /dev/null`
	if test "x$rtt" = "x"; then
	    rtt=65535
	fi
	if test `echo "scale=2 ; $rtt < $minrtt" | bc` -gt 0; then
	    minrtt=$rtt
	    nearhost=$host
	fi
    done
    if test "x$nearhost" = "x"; then
	echo "Repository servers unreachable.", $hosts
	echo "Check your internet connection. (or are you using proxy?)"
    nearhost=$default_reposerver
    fi
    reposerver=$nearhost
}


#---------------------------------------
# リポジトリサーバ
#---------------------------------------
create_srclist () {
    openrtm_repo="deb http://$reposerver/pub/Linux/debian/ $code_name main"
}

#---------------------------------------
# ソースリスト更新関数の定義
#---------------------------------------
update_source_list () {
    rtmsite=`grep $reposerver /etc/apt/sources.list`
    if test "x$rtmsite" = "x" ; then
	echo $openrtm_repo >> /etc/apt/sources.list
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

#----------------------------------------
# パッケージインストール関数
#----------------------------------------
install_packages () {
    for p in $*; do
	echo $msg9 $p　
	apt-get install -y --force-yes $p
	echo $msg10
	echo ""
    done
}

#------------------------------------------------------------
# リストを逆順にする
#------------------------------------------------------------
reverse () {
    for i in $*; do
	echo $i
    done | sed '1!G;h;$!d'
}

#----------------------------------------
# パッケージをアンインストールする
#----------------------------------------
uninstall_packages () {
    for p in $*; do
        echo $msg11 $p
        aptitude remove $p
        if test "$?" != 0; then
            apt-get purge $p
        fi
        echo $msg10
        echo ""
    done
}

#---------------------------------------
# メイン
#---------------------------------------
check_lang
check_root
check_codename
set_package_list

if test "x$1" = "x0.4.2" || test "x$1" = "x0.4" ; then
    openrtm=$openrtm04
    packages="$devel $omni $ace $pyyaml $openrtm"
fi

if test "x$1" = "x-u" ; then
    uninstall_packages `reverse $u_packages`
else
    check_reposerver
    create_srclist
    update_source_list
    apt-get autoclean
    apt-get update
#    uninstall_packages `reverse $openrtm`
    install_packages $packages
fi
