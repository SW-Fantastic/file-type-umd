package org.swdc.filetype.umd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class UMDFile {

    public static final String UMD_HEADER = "899b9ade";

    private File umdFile;

    private Map<UMDMetaType, String> metaData = new HashMap<>();

    private List<UMDChapter> chapters = new ArrayList<>();

    private byte[] textDecoded;

    private UMDType umdType;

    private Charset charset = StandardCharsets.UTF_16LE;

    private Function<byte[], String> decoder;

    public UMDFile(File file) {
        this.umdFile = file;
    }

    public UMDFile(File file, Function<byte[], String> textDecoder) {
        this.umdFile = file;
        this.decoder = textDecoder;
    }

    public String getAuthor() {
        return metaData.getOrDefault(UMDMetaType.Author, "Unknown");
    }

    public String getTitle() {
        return metaData.getOrDefault(UMDMetaType.Title, "Unknown");
    }

    public String getPublishDate() {
        return metaData.getOrDefault(UMDMetaType.PublishDay, "01");
    }

    public String getPublishMonth() {
        return metaData.getOrDefault(UMDMetaType.PublishMonth, "01");
    }

    public String getPublishYear() {
        return metaData.getOrDefault(UMDMetaType.PublishYear, "2022");
    }

    public String getBookType() {
        return metaData.getOrDefault(UMDMetaType.BookType, "Unknown");
    }

    public String getPublisher() {
        return metaData.getOrDefault(UMDMetaType.Publisher, "Unknown");
    }

    public String getVendor() {
        return metaData.getOrDefault(UMDMetaType.Vendor, "Unknown");
    }

    public UMDType getUmdType() {
        return umdType;
    }

    public boolean read() throws IOException {
        if (umdFile == null || !umdFile.exists() || umdFile.isDirectory()) {
            return false;
        }
        RandomAccessFile umdFile = new RandomAccessFile(this.umdFile, "rw");
        byte[] header = new byte[4];
        umdFile.read(header);
        String fileHeaderText = UMDUtil.computeHeader(header);
        if (fileHeaderText == null || !fileHeaderText.equals(UMD_HEADER)) {
            // not a umd file;
            return false;
        }
        umdFile.skipBytes(5);

        this.umdType = UMDType.cast(umdFile.readByte() & 0xFF);
        umdFile.skipBytes(2);
        // read metadata.
        int metaReadCount = 0;
        while (metaReadCount < UMDMetaType.values().length) {
            // 是0x23 = ‘#‘,但是这里跳过它。
            umdFile.skipBytes(1);
            // #后两个字节，为meta的type。
            byte[] meta = new byte[2];
            umdFile.read(meta);
            long metaType = UMDUtil.toUint16(meta);
            UMDMetaType metaDataType = UMDMetaType.cast(metaType);
            // #后第三个字节，无意义，跳过
            umdFile.skipBytes(1);
            // #后第四个字节，为内容长度 + 5
            int metaLen = (umdFile.readByte() & 0xFF) - 5;;
            // 读取metadata
            byte[] metaBytes = new byte[metaLen];
            umdFile.read(metaBytes);
            String metaValue = null;
            if (decoder != null) {
                metaValue = decoder.apply(metaBytes);
            } else {
                metaValue = new String(metaBytes, charset);
            }

            this.metaData.put(metaDataType,metaValue);
            metaReadCount ++;
        }

        // 跳过#字符
        umdFile.skipBytes(1);
        byte[] offsetSign = new byte[2];
        umdFile.read(offsetSign);

        // 跳过无效的11个字节
        umdFile.skipBytes(11);

        byte[] len = new byte[4];
        umdFile.read(len);
        long chapterOffsetsLength = (UMDUtil.toUint32(len) - 9) / 4;
        Map<Integer, Long> chapterOffsetMap = new HashMap<>();
        for (int idx = 0; idx < chapterOffsetsLength; idx ++) {
            byte[] offset = new byte[4];
            umdFile.read(offset);
            long offsetVal = UMDUtil.toUint32(offset);
            chapterOffsetMap.put(idx, offsetVal);
        }
        // umdFile.skipBytes(4 * (int)chapterOffsetsLength);
        // #
        umdFile.skipBytes(1);
        // 标题的标志码
        byte[] chSign = new byte[2];
        umdFile.read(chSign);
        // 11字节无效数据
        umdFile.skipBytes(11);
        // 4字节标题章节总长度
        byte[] chLen = new byte[4];
        umdFile.read(chLen);
        for (int idx = 0; idx < chapterOffsetsLength; idx ++) {
            // 标题长度（1字节）
            int charaTitleLen = (umdFile.read() & 0xFF);
            // 标题文本
            byte[] titleText = new byte[charaTitleLen];
            umdFile.read(titleText);
            String titleTextVal = new String(titleText, StandardCharsets.UTF_16LE);
            UMDChapter chapter = new UMDChapter();
            chapter.setChapterTitle(titleTextVal);
            chapter.setChapterOffset(chapterOffsetMap.get(idx).intValue());
            this.chapters.add(chapter);
        }

        ByteArrayOutputStream bot = new ByteArrayOutputStream();
        // 正文开始的Flag，值为0x24(十进制为36）
        int contentFlag = umdFile.read() & 0xFF;
        while (contentFlag == 36) {
            umdFile.skipBytes(4);
            byte[] compressedLength = new byte[4];
            umdFile.read(compressedLength);
            long chapterCompressedLength = UMDUtil.toUint32(compressedLength) - 9;

            byte[] chapCompressedData = new byte[(int)chapterCompressedLength];
            umdFile.read(chapCompressedData);
            byte[] decompressData = UMDUtil.decompress(chapCompressedData);
            if (decompressData != null) {
                bot.write(decompressData);
            } else {
                break;
            }
            long fp = umdFile.getFilePointer();
            contentFlag = umdFile.read() & 0xFF;
            if (contentFlag == 35) {
                byte[] flg = new byte[2];
                umdFile.read(flg);
                long flag = UMDUtil.toUint16(flg);
                if (flag == 10) {
                    // 0x0A
                    umdFile.skipBytes(6);
                    contentFlag = umdFile.read() & 0xFF;
                } else if (flag == 241) {
                    // 0xF1
                    umdFile.skipBytes(18);
                    contentFlag = umdFile.read() & 0xFF;
                } else {
                    umdFile.seek(fp);
                }
            } else if (contentFlag != 36){
                umdFile.seek(fp);
            }
        }
        textDecoded = bot.toByteArray();
        return true;
    }

    public byte[] getTextDecoded() {
        return textDecoded;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setDecoder(Function<byte[], String> decoder) {
        this.decoder = decoder;
    }

    public Function<byte[], String> getDecoder() {
        return decoder;
    }

    public List<UMDChapter> getChapters() {
        return chapters;
    }

    public UMDChapter getChapter(int idx) {
        return chapters.get(idx);
    }

    public UMDChapter getChapterText(UMDChapter chapter) {
        return getChapter(chapters.indexOf(chapter));
    }

    public String getChapterText(int chapter) {
        if (chapter >= chapters.size()) {
            return null;
        }
        UMDChapter curr = chapters.get(chapter);
        byte[] copied = null;
        if (chapter + 1 < chapters.size()) {
            UMDChapter next = chapters.get(chapter + 1);
            copied = Arrays.copyOfRange(textDecoded,curr.getChapterOffset(),next.getChapterOffset());
        } else {
            copied = Arrays.copyOfRange(textDecoded,curr.getChapterOffset(),textDecoded.length);
        }
        if (decoder != null) {
            return decoder.apply(copied);
        } else {
            return new String(copied,charset);
        }
    }
}
