
/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.core;

import org.wltea.analyzer.dic.Dictionary;
import org.wltea.analyzer.dic.Hit;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * 中文-日韩文子分词器
 */
class CJKSegmenter implements ISegmenter {

    //子分词器标签
    static final String SEGMENTER_NAME = "CJK_SEGMENTER";
    //待处理的分词hit队列 // ydd Trie查找到的前缀匹配结果
    private List<Hit> tmpHits;


    CJKSegmenter() {
        this.tmpHits = new LinkedList<Hit>();
    }

    /* (non-Javadoc)
     * @see org.wltea.analyzer.core.ISegmenter#analyze(org.wltea.analyzer.core.AnalyzeContext)
     */
    // TODO 他说的确实在理,是一次分词然后遍历分词结果。因为分词后需要消除歧义，是全部分词结果进行消除歧义。
    public void analyze(AnalyzeContext context) {
        if (CharacterUtil.CHAR_USELESS != context.getCurrentCharType()) {

            //优先处理tmpHits中的hit
            if (!this.tmpHits.isEmpty()) {  // ydd 前缀匹配
                //处理词段队列
                Hit[] tmpArray = this.tmpHits.toArray(new Hit[this.tmpHits.size()]);
                for (Hit hit : tmpArray) {
                    System.out.format("继续处理前缀匹配:%s \n", hit);
                    // TODO ydd 对已经通过前缀匹配过的hit继续往下遍历trie树,判断这些hit是否是词，不是则移除，是则放入结果集合。
                    hit = Dictionary.getSingleton().matchWithHit(context.getSegmentBuff(), context.getCursor(), hit);
                    System.out.format("继续处理前缀匹配-匹配结果:%s \n", hit);
                    if (hit.isMatch()) {  // TODO 主流程 3 // 匹配了trie树中的词
                        //输出当前的词
                        Lexeme newLexeme = new Lexeme(context.getBufferOffset(), hit.getBegin(), context.getCursor() - hit.getBegin() + 1, Lexeme.TYPE_CNWORD);
                        context.addLexeme(newLexeme); // TODO 把分词结果(前缀)发放入,原始分词结果集合，未经歧义处理。

                        if (!hit.isPrefix()) {//不是词前缀，hit不需要继续匹配，移除
                            this.tmpHits.remove(hit);
                        }

                    } else if (hit.isUnmatch()) {
                        //hit不是词，移除
                        this.tmpHits.remove(hit);
                    }
                }
            }

            //*********************************
            //再对当前指针位置的字符进行单字匹配
            Hit singleCharHit = Dictionary.getSingleton().matchInMainDict(context.getSegmentBuff(), context.getCursor(), 1);
            if (singleCharHit.isMatch()) {//首字成词
                //输出当前的词
                Lexeme newLexeme = new Lexeme(context.getBufferOffset(), context.getCursor(), 1, Lexeme.TYPE_CNWORD);
                context.addLexeme(newLexeme); // TODO 添加单字匹配结果  // 1.他 2.说 3.的 4.的确

                //同时也是词前缀
                if (singleCharHit.isPrefix()) {
                    //前缀匹配则放入hit列表
                    this.tmpHits.add(singleCharHit); // ydd
                }
            } else if (singleCharHit.isPrefix()) {//首字为词前缀  // TODO 前缀匹配
                //前缀匹配则放入hit列表
                this.tmpHits.add(singleCharHit);  //  ydd 匹配前缀记录。
            }


        } else {
            //遇到CHAR_USELESS字符
            //清空队列
            this.tmpHits.clear();
        }

        //判断缓冲区是否已经读完
        if (context.isBufferConsumed()) {
            //清空队列
            this.tmpHits.clear();
        }

        //判断是否锁定缓冲区
        if (this.tmpHits.size() == 0) {
            context.unlockBuffer(SEGMENTER_NAME);

        } else {
            context.lockBuffer(SEGMENTER_NAME);
        }
    }

    /* (non-Javadoc)
     * @see org.wltea.analyzer.core.ISegmenter#reset()
     */
    public void reset() {
        //清空队列
        this.tmpHits.clear();
    }

}
