require recipes-bsp/u-boot/u-boot-common.inc
require recipes-bsp/u-boot/u-boot.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

SRC_URI = " \
    git://github.com/smaeul/u-boot.git;protocol=https;branch=d1-wip \
    file://0001-riscv-fix-build-with-binutils-2.38.patch \
    file://tftp-mmc-boot.txt \
    file://uEnv-licheerv.txt \
    file://toc.cfg \
"

SRCREV = "afc07cec423f17ebb4448a19435292ddacf19c9b"

DEPENDS:append = " u-boot-tools-native python3-setuptools-native"

# Overwrite this for your server
TFTP_SERVER_IP ?= "127.0.0.1"

do_make_toc1_image[depends] = "opensbi:do_deploy"

do_configure:prepend() {
    sed -i -e 's,@SERVERIP@,${TFTP_SERVER_IP},g' ${WORKDIR}/tftp-mmc-boot.txt
    mkimage -O linux -T script -C none -n "U-Boot boot script" \
        -d ${WORKDIR}/tftp-mmc-boot.txt ${WORKDIR}/${UBOOT_ENV_BINARY}
}

# boot0 expects to load a TOC1 image containing OpenSBI and U-Boot
# (and a DTB). This is similar to, but incompatible with, mainline U-Boot
# SPL, which expects a FIT image.
do_make_toc1_image() {
    cd ${B}
    cp ${DEPLOY_DIR_IMAGE}/fw_dynamic.bin ${B}
    ${B}/tools/mkimage -T sunxi_toc1 -d ${WORKDIR}/toc.cfg ${B}/u-boot.toc1
}

do_deploy:append() {
    install -m 644 ${B}/u-boot.toc1 ${DEPLOYDIR}
    install -m 644 ${WORKDIR}/uEnv-licheerv.txt ${DEPLOYDIR}/uEnv.txt
}

COMPATIBLE_MACHINE = "(licheerv)"

TOOLCHAIN = "gcc"

addtask do_make_toc1_image before do_deploy after do_compile
