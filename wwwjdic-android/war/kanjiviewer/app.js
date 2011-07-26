jQuery(document).ready(function() {
    jQuery('#kanjiViewerParams').submit(function() {
        KanjiViewer.setFontSize(jQuery('#fontSize').val());
        KanjiViewer.setZoom(jQuery('#zoomFactor').val());
        KanjiViewer.setStrokeWidth(jQuery('#strokeWidth').val());
        return false;
    });
    KanjiViewer.initialize("kanjiViewer", jQuery('#strokeWidth').val(), jQuery('#fontSize').val(), kanjis);
});
