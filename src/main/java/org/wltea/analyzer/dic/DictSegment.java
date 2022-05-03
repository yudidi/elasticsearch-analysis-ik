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
package org.wltea.analyzer.dic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.elasticsearch.index.mapper.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 词典树分段，表示词典树的一个分枝  // TODO 附录1.0[前缀树，结构如下图所示](https://juejin.cn/post/6845166891120476168#heading-2)
 */
class DictSegment implements Comparable<DictSegment> {

    //公用字典表，存储汉字 // TODO 啥功能 A:推测是存放树中已经有的字符。那么k:v各代表什么呢?
    private static final Map<Character, Character> charMap = new ConcurrentHashMap<Character, Character>(16, 0.95f);
    //数组大小上限
    private static final int ARRAY_LENGTH_LIMIT = 3;


    //Map存储结构 // TODO ydd 这里就是trie,看三篇博客都是节点存放字符的实现方式  //  ydd 子节点数量>3,map存储
    private Map<Character, DictSegment> childrenMap;
    //数组方式存储结构 //  ydd 子节点<=3,数组存储
    private DictSegment[] childrenArray;


    //当前节点上存储的字符
    private Character nodeChar;
    //当前节点存储的Segment数目  // ydd 应该就是当前节点的子节点(DictSegment)数量? <=3,数组存储; >3,map存储
    //storeSize <=ARRAY_LENGTH_LIMIT ，使用数组存储， storeSize >ARRAY_LENGTH_LIMIT ,则使用Map存储
    private int storeSize = 0;
    //当前DictSegment状态 ,默认 0 , 1表示从根节点到当前节点的路径表示一个词
    private int nodeState = 0;


    DictSegment(Character nodeChar) {
        if (nodeChar == null) {
            throw new IllegalArgumentException("参数为空异常，字符不能为空");
        }
        this.nodeChar = nodeChar;
    }

    Character getNodeChar() {
        return nodeChar;
    }

    /*
     * 判断是否有下一个节点
     */
    boolean hasNextNode() {
        return this.storeSize > 0;
    }

    /**
     * 匹配词段
     *
     * @param charArray
     * @return Hit
     */
    Hit match(char[] charArray) {
        return this.match(charArray, 0, charArray.length, null);
    }

    /**
     * 匹配词段
     *
     * @param charArray
     * @param begin
     * @param length
     * @return Hit
     */
    Hit match(char[] charArray, int begin, int length) {
        return this.match(charArray, begin, length, null);
    }

    /**
     * 匹配词段
     *
     * @param charArray
     * @param begin
     * @param length
     * @param searchHit
     * @return Hit
     */
    Hit match(char[] charArray, int begin, int length, Hit searchHit) {
        if (searchHit == null) {
            //如果hit为空，新建
            searchHit = new Hit();
            //设置hit的其实文本位置
            searchHit.setBegin(begin);
        } else {
            //否则要将HIT状态重置
            searchHit.setUnmatch();
        }
        //设置hit的当前处理位置
        searchHit.setEnd(begin);

        Character keyChar = Character.valueOf(charArray[begin]);
        DictSegment ds = null;

        //引用实例变量为本地变量，避免查询时遇到更新的同步问题
        DictSegment[] segmentArray = this.childrenArray;
        Map<Character, DictSegment> segmentMap = this.childrenMap;

        //STEP1 在节点中查找keyChar对应的DictSegment
        if (segmentArray != null) {  // ydd 有2种分叉,不是array就是map
            //在数组中查找
            DictSegment keySegment = new DictSegment(keyChar);
            int position = Arrays.binarySearch(segmentArray, 0, this.storeSize, keySegment);
            if (position >= 0) {
                ds = segmentArray[position];
            }

        } else if (segmentMap != null) {
            //在map中查找
            ds = (DictSegment) segmentMap.get(keyChar);
        }

        System.out.format("trie中查找到匹配记录,-DictSegment:%s \n", ds);

        //STEP2 找到DictSegment，判断词的匹配状态，是否继续递归，还是返回结果
        if (ds != null) {
            if (length > 1) {
                //词未匹配完，继续往下搜索
                return ds.match(charArray, begin + 1, length - 1, searchHit); // ydd 递归查找
            } else if (length == 1) {

                //搜索最后一个char
                if (ds.nodeState == 1) {
                    //添加HIT状态为完全匹配
                    searchHit.setMatch();
                }
                if (ds.hasNextNode()) {
                    //添加HIT状态为前缀匹配
                    searchHit.setPrefix();
                    //记录当前位置的DictSegment
                    searchHit.setMatchedDictSegment(ds);
                }
                return searchHit;
            }

        }
        //STEP3 没有找到DictSegment， 将HIT设置为不匹配
        return searchHit;
    }

    /**
     * 加载填充词典片段
     *
     * @param charArray
     */
    void fillSegment(char[] charArray) {
        this.fillSegment(charArray, 0, charArray.length, 1);
    }

    /**
     * 屏蔽词典中的一个词
     *
     * @param charArray
     */
    void disableSegment(char[] charArray) {
        this.fillSegment(charArray, 0, charArray.length, 0);
    }

    /**
     * 加载填充词典片段 // ydd 附录1.0 把词加入到字典中的逻辑
     *
     * @param charArray
     * @param begin
     * @param length
     * @param enabled   // ydd trie的词尾节点
     */
    private synchronized void fillSegment(char[] charArray, int begin, int length, int enabled) {
        //获取字典表中的汉字对象
        Character beginChar = Character.valueOf(charArray[begin]);
        Character keyChar = charMap.get(beginChar); // ydd 获取词的首字母的对应字?  // 这个好像是跟节点的孩子节点,用map存储
        //字典中没有该字，则将其添加入字典
        if (keyChar == null) {
            charMap.put(beginChar, beginChar); // ydd 总是设置k-v相同的东西 k:词的首字母 v:词的首字母
            keyChar = beginChar;
        }

        //搜索当前节点的存储，查询对应keyChar的keyChar，如果没有则创建
        DictSegment ds = lookforSegment(keyChar, enabled);
        if (ds != null) {
            //处理keyChar对应的segment
            if (length > 1) {
                //词元还没有完全加入词典树
                ds.fillSegment(charArray, begin + 1, length - 1, enabled);
            } else if (length == 1) {
                //已经是词元的最后一个char,设置当前节点状态为enabled，
                //enabled=1表明一个完整的词，enabled=0表示从词典中屏蔽当前词
                ds.nodeState = enabled;
            }
        }

    }

    /**
     * 查找本节点下对应的keyChar的segment	 *
     *
     * @param keyChar
     * @param create  =1如果没有找到，则创建新的segment ; =0如果没有找到，不创建，返回null
     * @return
     */
    private DictSegment lookforSegment(Character keyChar, int create) {

        DictSegment ds = null;

        if (this.storeSize <= ARRAY_LENGTH_LIMIT) {// ydd 当前分叉的子节点数目小于3个
            //获取数组容器，如果数组未创建则创建数组
            DictSegment[] segmentArray = getChildrenArray();
            //搜寻数组
            DictSegment keySegment = new DictSegment(keyChar);
            int position = Arrays.binarySearch(segmentArray, 0, this.storeSize, keySegment);
            if (position >= 0) {
                ds = segmentArray[position];
            }

            //遍历数组后没有找到对应的segment
            if (ds == null && create == 1) {
                ds = keySegment;
                if (this.storeSize < ARRAY_LENGTH_LIMIT) {
                    //数组容量未满，使用数组存储
                    segmentArray[this.storeSize] = ds;
                    //segment数目+1
                    this.storeSize++;
                    Arrays.sort(segmentArray, 0, this.storeSize);

                } else {
                    //数组容量已满，切换Map存储
                    //获取Map容器，如果Map未创建,则创建Map
                    Map<Character, DictSegment> segmentMap = getChildrenMap();
                    //将数组中的segment迁移到Map中
                    migrate(segmentArray, segmentMap);
                    //存储新的segment
                    segmentMap.put(keyChar, ds);
                    //segment数目+1 ，  必须在释放数组前执行storeSize++ ， 确保极端情况下，不会取到空的数组
                    this.storeSize++;
                    //释放当前的数组引用
                    this.childrenArray = null;
                }

            }

        } else { // TODO 优先看这个逻辑,
            //获取Map容器，如果Map未创建,则创建Map
            Map<Character, DictSegment> segmentMap = getChildrenMap();
            //搜索Map
            ds = (DictSegment) segmentMap.get(keyChar);
            if (ds == null && create == 1) {
                //构造新的segment
                ds = new DictSegment(keyChar);
                segmentMap.put(keyChar, ds);
                //当前节点存储segment数目+1
                this.storeSize++;
            }
        }

        return ds;
    }


    /**
     * 获取数组容器
     * 线程同步方法
     */
    private DictSegment[] getChildrenArray() {
        synchronized (this) {
            if (this.childrenArray == null) {
                this.childrenArray = new DictSegment[ARRAY_LENGTH_LIMIT];
                //System.out.println("懒初始化,childrenArray有3个null元素,storesize=0,nodeState应该是1表示词尾" + this.nodeState);
            }
        }
        return this.childrenArray;
    }

    /**
     * 获取Map容器
     * 线程同步方法
     */
    private Map<Character, DictSegment> getChildrenMap() {
        synchronized (this) {
            if (this.childrenMap == null) {
                this.childrenMap = new ConcurrentHashMap<Character, DictSegment>(ARRAY_LENGTH_LIMIT * 2, 0.8f);
            }
        }
        return this.childrenMap;
    }

    /**
     * 将数组中的segment迁移到Map中
     *
     * @param segmentArray
     */
    private void migrate(DictSegment[] segmentArray, Map<Character, DictSegment> segmentMap) {
        for (DictSegment segment : segmentArray) {
            if (segment != null) {
                segmentMap.put(segment.nodeChar, segment);
            }
        }
    }

    /**
     * 实现Comparable接口
     *
     * @param o
     * @return int
     */
    public int compareTo(DictSegment o) {
        //对当前节点存储的char进行比较
        return this.nodeChar.compareTo(o.nodeChar);
    }

    // ydd for debug
    @Override
    public String toString() {
        //return "DictSegment{" +
        //        ", nodeChar=" + nodeChar +
        //        ", storeSize=" + storeSize +
        //        ", nodeState=" + nodeState +
        //        ", childrenMap=" + childrenMap +
        //        ", childrenArray=" + childrenArraySimpleString(childrenArray) +
        //        //", childrenArray=" + Arrays.toString(childrenArray) +
        //        '}';
        //String pretty = JSON.toJSONString(this, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
        //        SerializerFeature.WriteDateUseDateFormat);
        //return pretty;
        //String resultJson = JSONObject.toJSONString(this);
        //1、使用JSONObject
        //JSONObject json = JSONObject.(this);
        //return json.toJSONString();

        JSONObject object = new JSONObject();
        //string
        object.put("nodeChar", nodeChar);
        //int
        object.put("storeSize", storeSize);
        //boolean
        object.put("nodeState", nodeState);
        object.put("childrenMap", childrenMap);
        //array
        if (childrenArray == null) {
            object.put("childrenArray", null);
        } else {
            //object.put("childrenArray", Arrays.asList(childrenArray));
            object.put("childrenArray", childrenArraySimpleString(childrenArray));

            // TODO
            //for (int i = 0; i < childrenArray.length; i++) {
            //
            //}
        }
        //null
        //object.put("null",null);
        //System.out.println(object);
        return JSON.toJSONString(object, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat);
    }

    private String childrenArraySimpleString(DictSegment[] childrenArray) {
        if (childrenArray == null) {
            return "";
        }
        String s = "[";
        for (int i = 0; i < childrenArray.length; i++) {
            if (childrenArray[i] == null) {
                s += "null, ";
            } else {
                s += "DictSegment{nodeChar=" + childrenArray[i].nodeChar + "," + "nodeState=" + childrenArray[i].nodeState + "}, ";
            }
        }
        s += "]";
        return s;
    }

    // ydd 每次添加词语,打印这课树
    public void print(Integer height) {
        //String resultJson = JSONObject.toJSONString(this);
        //System.out.println(resultJson);
        //System.out.println(JSON.toJSONString(this, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
        //        SerializerFeature.WriteDateUseDateFormat));
        System.out.println("----------Start Print---------- 新分叉DictSegment,height:" + height + ",DictSegment:" + this);
        if (this.storeSize <= ARRAY_LENGTH_LIMIT) { // 这个if和else参考fillSegment()方法中的lookforSegment()
            DictSegment[] segmentArray = getChildrenArray();
            if (this.storeSize > 0) {
                for (int i = 0; i < segmentArray.length; i++) {
                    //System.out.println("height:" + height.toString() + ",第" + i + "个孩子 " + segmentArray[i]);
                    if (segmentArray[i] == null) {
                        continue;
                    }
                    //if (segmentArray[i].nodeState == 1) { // 词尾
                    //    System.out.println("  词尾结束递归: " + segmentArray[i]);
                    //} else { // 词中,则继续遍历
                    segmentArray[i].print(height + 1);
                    //}
                }
            }
        } else {
            Map<Character, DictSegment> segmentMap = getChildrenMap();
            for (Map.Entry<Character, DictSegment> entry : segmentMap.entrySet()) {
                //System.out.println("height:" + height.toString() + " " + "Key = " + entry.getKey() + ", Value = " + entry.getValue());
                //if (entry.getValue().nodeState == 1) { // 词尾
                //    System.out.println("  词尾:" + entry.getValue());
                //} else { // 词中,则继续遍历
                entry.getValue().print(height + 1);
                //}
            }
        }
    }
}
