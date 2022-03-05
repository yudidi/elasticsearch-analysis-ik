
# 详解了ik源码思路和代码的对应关系
// 附录1
3.1 词典构造
3.2 分词
3.3 消除歧义: 也就是说，在第三步这边，ik_smart会对结果比ik_max_word多一个筛选的过程。
3.4 输出结果: 该部分的主要逻辑，就是对于那些没有被分词分到的位置，用单字输出的方式输出词元。

## tokenizer、analyzer
附录1.1

# 附录
1.[IK分词器源码阅读笔记](https://www.aramigo.ltd/index.php/archives/88/)

1.1 AnalysisIkPlugin.java中的tokenizer、analyzer [es中的analyzer，tokenizer，filter你真的了解吗？](https://cloud.tencent.com/developer/article/1851823?from=article.detail.1706529)