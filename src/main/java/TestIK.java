import org.elasticsearch.common.settings.Settings;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;
import java.io.StringReader;

public class TestIK {

    public static void main(String[] args) throws IOException {
        // ik的切词方式主要有两种，一种为smart模式，一种为ik_max_word即非smart模式
//        testIkSegment();
//        testIkSegmentSmart();

        testIkSegmentNotSmart消除歧义();
    }

    /*
    对应主词典内容:
    得饶人处
    得饶人处且饶人
    饶人
     */

    /*
TODO
      Q:不知道为啥可以分出且,处等词, 字典明明没有这个词.
      A:附录
      首先调用AnalyzeContext.fillBuffer(this.input)从Reader读取8K数据到到segmentBuff的char数组中，
      然后调用子分词器CJKSegmenter（中日韩文分词器），
      CN_QuantifierSegmenter（中文数量词分词器），
      LetterSegmenter（英文分词器）的analyze方法依次从头处理segmentBuff中的每一个字符。// => 这里产生了g字母

      LetterSegmenter.analyze()：英文分词器逻辑很简单，从segmentBuff中遇到第一个英文字符往后，直到碰到第一个非英文字符，这中间的所有字符则切分为一个英文单词。
      CN_QuantifierSegmenter.analyze()：中文量词分词器处理逻辑也很简单，在segmentBuff中遇到每一个中文数量词，然后检查该数量词后一个字符是否未中文量词（根据是否包含在中文量词词典中为判断依据），如是，则分成一个词，如否，则不是一个词。
     */

    public static void testIkSegment() throws IOException {
        String t = "得饶人处且饶人<g>";
        Settings settings = Settings.builder()
                .put("use_smart", false)
                .put("enable_lowercase", false)
                .put("enable_remote_dict", false)
                .build();
        Configuration configuration = new Configuration(null, settings).setUseSmart(false); // 读取配置，加载词典
        IKSegmenter segmenter = new IKSegmenter(new StringReader(t), configuration);
        Lexeme next;
        while ((next = segmenter.next()) != null) {
            System.out.println(next.getLexemeText());
        }
    }

    public static void testIkSegmentSmart() throws IOException {
        String t = "得饶人处且饶人<g>";
        Settings settings = Settings.builder()
                .put("use_smart", true)
                .put("enable_lowercase", false)
                .put("enable_remote_dict", false)
                .build();
        Configuration configuration = new Configuration(null, settings).setUseSmart(true); // 读取配置，加载词典 // 这个setUseSmart多余了
        IKSegmenter segmenter = new IKSegmenter(new StringReader(t), configuration);
        Lexeme next;
        while ((next = segmenter.next()) != null) {
            System.out.println(next.getLexemeText());
        }
    }

    public static void testIkSegmentNotSmart消除歧义() throws IOException {
        String t = "耐克鞋";
        Settings settings = Settings.builder()
                .put("use_smart", false)
                .put("enable_lowercase", false)
                .put("enable_remote_dict", false)
                .build();
        Configuration configuration = new Configuration(null, settings).setUseSmart(true); // 读取配置，加载词典 // 这个setUseSmart多余了
        IKSegmenter segmenter = new IKSegmenter(new StringReader(t), configuration);
        Lexeme next;
        while ((next = segmenter.next()) != null) {
            System.out.println(next.getLexemeText());
        }
    }
}
