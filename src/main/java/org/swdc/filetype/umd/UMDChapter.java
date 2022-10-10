package org.swdc.filetype.umd;

public class UMDChapter {

    private String chapterTitle;

    private int chapterOffset;

    public void setChapterOffset(int chapterOffset) {
        this.chapterOffset = chapterOffset;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public int getChapterOffset() {
        return chapterOffset;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }
}
