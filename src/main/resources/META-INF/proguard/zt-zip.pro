# make sure ZipExtraField constructors are kept
-keep class * implements org.zeroturnaround.zip.extra.ZipExtraField {
    public <init>();
}
