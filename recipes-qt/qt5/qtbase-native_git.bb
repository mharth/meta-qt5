DESCRIPTION = "Native version of Qt/[X11|Mac|Embedded]"
DEPENDS = "zlib-native dbus-native"
SECTION = "libs"
HOMEPAGE = "http://qt-project.org"

LICENSE = "GFDL-1.3 & BSD & ( GPL-3.0 & The-Qt-Company-GPL-Exception-1.0 | The-Qt-Company-Commercial ) & ( GPL-2.0+ | LGPL-3.0 | The-Qt-Company-Commercial )"
LIC_FILES_CHKSUM = " \
    file://LICENSE.LGPL3;md5=e6a600fd5e1d9cbde2d983680233ad02 \
    file://LICENSE.GPL2;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
    file://LICENSE.GPL3;md5=d32239bcb673463ab874e80d47fae504 \
    file://LICENSE.GPL3-EXCEPT;md5=763d8c535a234d9a3fb682c7ecb6c073 \
    file://LGPL_EXCEPTION.txt;md5=9625233da42f9e0ce9d63651a9d97654 \
    file://LICENSE.FDL;md5=6d9f2a9af4c8b8c3c769f6cc1b6aaf7e \
    file://LICENSE.PREVIEW.COMMERCIAL;md5=8ee24b8d305ef7779e07647a7b70e1bc \
"

require qt5-native.inc
require qt5-git.inc

# common for qtbase-native, qtbase-nativesdk and qtbase
# Patches from https://github.com/meta-qt5/qtbase/commits/b5.11-shared
# 5.11.meta-qt5-shared.7
SRC_URI += "\
    file://0001-Add-linux-oe-g-platform.patch \
    file://0002-cmake-Use-OE_QMAKE_PATH_EXTERNAL_HOST_BINS.patch \
    file://0003-qlibraryinfo-allow-to-set-qt.conf-from-the-outside-u.patch \
    file://0004-configure-bump-path-length-from-256-to-512-character.patch \
    file://0005-Disable-all-unknown-features-instead-of-erroring-out.patch \
    file://0006-Pretend-Qt5-wasn-t-found-if-OE_QMAKE_PATH_EXTERNAL_H.patch \
    file://0007-Delete-qlonglong-and-qulonglong.patch \
    file://0008-Replace-pthread_yield-with-sched_yield.patch \
    file://0009-Add-OE-specific-specs-for-clang-compiler.patch \
    file://0010-linux-clang-Invert-conditional-for-defining-QT_SOCKL.patch \
    file://0011-tst_qlocale-Enable-QT_USE_FENV-only-on-glibc.patch \
    file://0012-mkspecs-common-gcc-base.conf-Use-I-instead-of-isyste.patch \
    file://0013-Upgrade-double-conversion-to-v3.0.0.patch \
    file://0014-Fix-qmake-build-with-glibc-2.28.patch \
"

# common for qtbase-native and nativesdk-qtbase
# Patches from https://github.com/meta-qt5/qtbase/commits/b5.10-native
# 5.11.meta-qt5-native.7
SRC_URI += " \
    file://0015-Always-build-uic-and-qvkgen.patch \
"

# only for qtbase-native
SRC_URI += " \
    file://0016-Bootstrap-without-linkat-feature.patch \
"

CLEANBROKEN = "1"

XPLATFORM_toolchain-clang = "linux-oe-clang"
XPLATFORM ?= "linux-oe-g++"

PACKAGECONFIG_CONFARGS = " \
    -sysroot ${STAGING_DIR_NATIVE} \
    -no-gcc-sysroot \
    -system-zlib \
    -qt-pcre \
    -no-libjpeg \
    -no-libpng \
    -no-gif \
    -no-accessibility \
    -no-cups \
    -no-gui \
    -no-sql-mysql \
    -no-sql-sqlite \
    -no-opengl \
    -no-openssl \
    -no-xcb \
    -no-icu \
    -verbose \
    -release \
    -prefix ${OE_QMAKE_PATH_PREFIX} \
    -hostprefix ${OE_QMAKE_PATH_PREFIX} \
    -bindir ${OE_QMAKE_PATH_BINS} \
    -hostbindir ${OE_QMAKE_PATH_BINS} \
    -libdir ${OE_QMAKE_PATH_LIBS} \
    -hostlibdir ${OE_QMAKE_PATH_LIBS} \
    -headerdir ${OE_QMAKE_PATH_HEADERS} \
    -archdatadir ${OE_QMAKE_PATH_ARCHDATA} \
    -datadir ${OE_QMAKE_PATH_DATA} \
    -hostdatadir ${QMAKE_MKSPEC_PATH_NATIVE} \
    -docdir ${OE_QMAKE_PATH_DOCS} \
    -sysconfdir ${OE_QMAKE_PATH_SETTINGS} \
    -no-glib \
    -no-iconv \
    -silent \
    -nomake examples \
    -nomake tests \
    -no-rpath \
    -no-feature-linkat \
    -platform ${XPLATFORM} \
"

# for qtbase configuration we need default settings
# since we cannot set empty set filename to a not existent file
deltask generate_qt_config_file

do_configure_prepend() {
    # Regenerate header files when they are included in source tarball
    # Otherwise cmake files don't set PRIVATE_HEADERS correctly
    rm -rf ${S}/include
    mkdir -p ${S}/.git || true

    # Avoid qmake error "Cannot read [...]/usr/lib/qt5/mkspecs/oe-device-extra.pri: No such file or directory"
    touch ${S}/mkspecs/oe-device-extra.pri

    MAKEFLAGS="${PARALLEL_MAKE}" ${S}/configure -${QT_EDITION} -confirm-license ${PACKAGECONFIG_CONFARGS} || die "Configuring qt failed. PACKAGECONFIG_CONFARGS was ${PACKAGECONFIG_CONFARGS}"
}

do_install() {
    # Fix install paths for all
    find . -name "Makefile*" | xargs sed -i "s,(INSTALL_ROOT)${STAGING_DIR_NATIVE}${STAGING_DIR_NATIVE},(INSTALL_ROOT)${STAGING_DIR_NATIVE},g"

    oe_runmake install INSTALL_ROOT=${D}

    if [ -d ${D}${STAGING_DIR_NATIVE}${STAGING_DIR_NATIVE} ] ; then
        echo "Some files are installed in wrong directory ${D}${STAGING_DIR_NATIVE}"
        cp -ra ${D}${STAGING_DIR_NATIVE}${STAGING_DIR_NATIVE}/* ${D}${STAGING_DIR_NATIVE}
        rm -rf ${D}${STAGING_DIR_NATIVE}${STAGING_DIR_NATIVE}
        # remove empty dirs
        TMP=`dirname ${D}/${STAGING_DIR_NATIVE}${STAGING_DIR_NATIVE}`
        while test ${TMP} != ${D}${STAGING_DIR_NATIVE}; do
            rmdir ${TMP}
            TMP=`dirname ${TMP}`;
        done
    fi

    install -m 755 ${B}/bin/qfloat16-tables ${D}${OE_QMAKE_PATH_BINS}

    # since 5.9.2 something sets a very strange path to mkspec ("${_qt5Core_install_prefix}/../../../../../../../../../../usr/lib/qt5//mkspecs/linux-oe-g++")
    # override this until somebody finds a better way
    echo 'set(_qt5_corelib_extra_includes "${_qt5Core_install_prefix}/lib${QT_DIR_NAME}/mkspecs/linux-oe-g++")' > ${D}${libdir}/cmake/Qt5Core/Qt5CoreConfigExtrasMkspecDir.cmake
}

SRCREV = "74305ba470f48da8b4c4e806fc714fe9f7649156"
