package com.chatopera.clause;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Unit test for Chatopera Clause.
 */
public class ClauseClientTest
        extends TestCase {
    private final static Logger logger = LoggerFactory.getLogger(ClauseClientTest.class);

    // variables
    private TTransport transport;
    private Serving.Client client;

    // constants
    private final static String CLAUSE_IP = "gamera";
    private final static int CLAUSE_PORT = 17056;

    // 字母开头的字符串[a-zA-Z0-9_]
    private final static String CHATBOT_ID = "avtr002";

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ClauseClientTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ClauseClientTest.class);
    }


    @Override
    public void setUp() throws TTransportException {
        this.transport = new TFramedTransport(new TSocket(CLAUSE_IP, CLAUSE_PORT));
        TProtocol protocol = new TBinaryProtocol(transport);

        this.client = new Serving.Client(protocol);
        this.transport.open(); // 建立连接
    }

    @Override
    public void tearDown() {
        if (transport != null) {
            transport.close();
        }
    }

    private void destroyChatbotById(final String chatbotID) throws TException {
        logger.info("[destroyChatbotById] chatbotID {}", chatbotID);
        Data response;
        Data request = new Data();
        request.chatbotID = chatbotID;
        response = client.getIntents(request);

        // 删除意图，该接口会删除关联的意图说法和槽位
        for (final Intent intent : response.intents) {
            request = new Data();
            request.id = intent.getId();
            client.delIntent(request);
        }


        // 删除自定义词典
        request = new Data();
        request.chatbotID = chatbotID;
        response = client.getCustomDicts(request);
        for (final Dict dict : response.customdicts) {
            request.customdict = dict;
            client.delCustomDict(request);
        }

        // 取消引用系统词典
        request = new Data();
        request.chatbotID = chatbotID;
        response = client.mySysdicts(request);
        for (final Dict dict : response.sysdicts) {
            request.sysdict = dict;
            client.unrefSysDict(request);
        }
    }

    /**
     * 删除机器人
     */
    public void testDestroyBot() throws TException {
        destroyChatbotById(CHATBOT_ID);
    }


    /**
     * 创建，训练和请求机器对话
     */
    public void testCreateBot() throws InterruptedException, TException {

        // 删除旧的机器人
        // NOTE: 此处只是方便重复的演示后面的代码，实际使用中，建议不删除机器人。
        // 删除机器人将其意图、词典、词条、槽位等信息擦除。
        destroyChatbotById(CHATBOT_ID);

        final String intentName = "orderTakeOut";

        // 创建自定义词典, 词表类型
        Data response;
        Data request = new Data();
        Dict termsdict = new Dict();
        termsdict.name = "food";
        termsdict.type = "vocab";
        termsdict.chatbotID = CHATBOT_ID;
        request.customdict = termsdict;
        response = client.postCustomDict(request);
        logger.info("postCustomDict >> \n{}", response.toString());

        // 在自定义词典中添加词条
        request = new Data();
        DictWord word = new DictWord();
        word.word = "西红柿";
        word.synonyms = "狼桃;柿子;番茄";
        request.customdict = termsdict;
        request.dictword = word;
        request.chatbotID = CHATBOT_ID;
        response = client.putDictWord(request);
        logger.info("putDictWord >> \n{}", response.toString());

        // 创建自定义词典，正则表达式类型
        request = new Data();
        Dict patterndict = new Dict();
        patterndict.name = "phoneNumber";
        patterndict.type = "regex";
        patterndict.chatbotID = CHATBOT_ID;
        request.customdict = patterndict;
        response = client.postCustomDict(request);
        logger.info("postCustomDict >> \n{}", response.toString());

        request = new Data();
        request.customdict = patterndict;
        DictPattern dictPattern = new DictPattern();
        dictPattern.patterns = new ArrayList<>(Arrays.asList("[1]([3-9])[0-9]{9}"));
        request.dictpattern = dictPattern;
        client.putDictPattern(request);
        response = client.putDictPattern(request);
        logger.info("putDictPattern >> \n{}", response.toString());


        // 引用系统词典
        request = new Data();
        Dict sysdict = new Dict();
        sysdict.name = "@LOC";
        request.sysdict = sysdict;
        request.chatbotID = CHATBOT_ID;
        response = client.refSysDict(request);
        logger.info("refSysDict >> \n{}", response.toString());

        // 引用系统词典
        request = new Data();
        sysdict = new Dict();
        sysdict.name = "@TIME";
        request.sysdict = sysdict;
        request.chatbotID = CHATBOT_ID;
        response = client.refSysDict(request);
        logger.info("refSysDict >> \n{}", response.toString());

        // 创建意图
        request = new Data();
        final Intent intent = new Intent();
        intent.chatbotID = CHATBOT_ID;
        intent.name = intentName;
        request.intent = intent;
        response = client.postIntent(request);
        logger.info("postIntent >> \n{}", response.toString());


        // 创建意图槽位：配菜
        request = new Data();
        IntentSlot slot = new IntentSlot();
        slot.name = "vegetable";
        slot.requires = true;
        slot.setRequires(true);
        slot.question = "您需要什么配菜";
        Dict slotDict1 = new Dict();
        slotDict1.chatbotID = CHATBOT_ID;
        slotDict1.name = "food";
        request.intent = intent;
        request.slot = slot;
        request.customdict = slotDict1;
        response = client.postSlot(request);
        logger.info("postSlot >> \n{}", response.toString());

        // 创建意图槽位：送达位置
        request = new Data();
        slot = new IntentSlot();
        slot.name = "location";
        slot.requires = true;
        slot.setRequires(true);
        slot.question = "外卖送到哪里";
        Dict slotDict2 = new Dict();
        slotDict2.name = "@LOC";
        request.intent = intent;
        request.sysdict = slotDict2;
        request.slot = slot;
        response = client.postSlot(request);
        logger.info("postSlot >> \n{}", response.toString());


        // 创建意图槽位：送达时间
        request = new Data();
        slot = new IntentSlot();
        slot.name = "date";
        slot.requires = true;
        slot.setRequires(true);
        slot.question = "您希望什么时候用餐";
        Dict slotDict3 = new Dict();
        slotDict3.name = "@TIME";
        request.intent = intent;
        request.sysdict = slotDict3;
        request.slot = slot;
        response = client.postSlot(request);
        logger.info("postSlot >> \n{}", response.toString());

        // 创建意图槽位：手机号
        request = new Data();
        slot = new IntentSlot();
        slot.name = "phone";
        slot.requires = true;
        slot.setRequires(true);
        slot.question = "您的手机号是什么";
        Dict slotDict4 = new Dict();
        slotDict4.name = "phoneNumber";
        slotDict4.chatbotID = CHATBOT_ID;
        request.intent = intent;
        request.customdict = slotDict4;
        request.slot = slot;
        response = client.postSlot(request);
        logger.info("postSlot >> \n{}", response.toString());

        // 添加意图说法
        request = new Data();
        IntentUtter utter = new IntentUtter();
        utter.utterance = "我想点外卖";
        request.intent = intent;
        request.utter = utter;
        response = client.postUtter(request);
        logger.info("postUtter >> \n{}", response.toString());

        // 添加意图说法
        request = new Data();
        IntentUtter utter1 = new IntentUtter();
        utter1.utterance = "帮我来一份{vegetable}，送到{location}";
        request.intent = intent;
        request.utter = utter1;
        response = client.postUtter(request);
        logger.info("postUtter >> \n{}", response.toString());


        // 添加意图说法
        request = new Data();
        IntentUtter utter2 = new IntentUtter();
        utter2.utterance = "帮我来一份{vegetable}，送到{location}，我的手机号是{phone}";
        request.intent = intent;
        request.utter = utter2;
        response = client.postUtter(request);
        logger.info("postUtter >> \n{}", response.toString());

        /**
         * 训练机器人
         */
        request = new Data();
        request.chatbotID = CHATBOT_ID;
        response = client.train(request);
        logger.info("train >> \n{}", response.toString());

        // 训练是一个长时任务，进行异步反馈


        while (true) {
            Thread.sleep(1000 * 3);
            response = client.status(request);
            if (response.rc == 0) {
                break;
            }
        }

        /**
         * 与机器人对话
         */
        // 创建 session
        request = new Data();
        ChatSession session = new ChatSession();
        session.chatbotID = CHATBOT_ID;
        session.uid = "java"; // 用户唯一的标识
        session.channel = "testclient"; // 自定义，代表该用户渠道由字母组成
        session.branch = "dev"; // 测试分支，有连个选项：dev, 测试分支；pro，生产分支
        request.session = session;

        final Data s = client.putSession(request);
        logger.info("putSession >> \n{}", s.toString());

        // 发送消息
        request = new Data();
        request.session = s.session;
        ChatMessage message = new ChatMessage();
        message.textMessage = "帮我来一份番茄，送到五道口20号1单元，手机号是15888888888";
        request.message = message;
        response = client.chat(request);
        logger.info("chat >> \n{}", response.toString());

//        Data(rc:0, session:ChatSession(intent_name:orderTakeOut, chatbotID:avtr002,
//        uid:java, channel:testclient, resolved:false, id:A7393F609AA2BA94E6326E9320495C96,
//        entities:
//        [Entity(name:phone, val:15888888888, requires:true, dictname:phoneNumber),
//        Entity(name:date, val:, requires:true, dictname:@TIME),
//        Entity(name:vegetable, val:番茄, requires:true, dictname:food),
//        Entity(name:location, val:五道口20号1单元, requires:true, dictname:@LOC)],
//        branch:dev, createdate:2019-12-16 16:30:02, updatedate:2019-12-16 16:30:02),
//        message:ChatMessage(receiver:java, textMessage:您希望什么时候用餐,
//        is_fallback:false, is_proactive:true))

    }
}
