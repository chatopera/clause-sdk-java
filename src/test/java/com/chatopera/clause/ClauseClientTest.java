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

/**
 * Unit test for Chatopera Clause.
 */
public class ClauseClientTest
        extends TestCase {
    private final static Logger logger = LoggerFactory.getLogger(ClauseClientTest.class);

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


    /**
     * Build bot and chat
     */
    public void testApp() throws InterruptedException {
        final String CLAUSE_IP = "127.0.0.1";
        final int CLAUSE_PORT = 8056;
        final String chatbotID = "avtr002";
        final String intentName = "orderTakeOut";


        TTransport transport;
        try {
            transport = new TFramedTransport(new TSocket(CLAUSE_IP, CLAUSE_PORT));

            TProtocol protocol = new TBinaryProtocol(transport);

            Serving.Client client = new Serving.Client(protocol);
            transport.open(); // 建立连接

            // 创建自定义词典
            Data response;
            Data request = new Data();
            Dict customdict = new Dict();
            customdict.name = "food";
            customdict.chatbotID = chatbotID;
            request.customdict = customdict;
            response = client.postCustomDict(request);
            logger.info("postCustomDict >> \n{}", response.toString());


            // 在自定义词典中添加词条
            request = new Data();
            DictWord word = new DictWord();
            word.word = "西红柿";
            word.synonyms = "狼桃;柿子;番茄";
            request.customdict = customdict;
            request.dictword = word;
            request.chatbotID = chatbotID;
            response = client.putDictWord(request);
            logger.info("putDictWord >> \n{}", response.toString());

            // 引用系统词典
            request = new Data();
            Dict sysdict = new Dict();
            sysdict.name = "@LOC";
            request.sysdict = sysdict;
            request.chatbotID = chatbotID;
            response = client.refSysDict(request);
            logger.info("refSysDict >> \n{}", response.toString());

            // 引用系统词典
            request = new Data();
            sysdict = new Dict();
            sysdict.name = "@TIME";
            request.sysdict = sysdict;
            request.chatbotID = chatbotID;
            response = client.refSysDict(request);
            logger.info("refSysDict >> \n{}", response.toString());

            // 创建意图
            request = new Data();
            final Intent intent = new Intent();
            intent.chatbotID = chatbotID;
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
            slotDict1.chatbotID = chatbotID;
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

            /**
             * 训练机器人
             */
            request = new Data();
            request.chatbotID = chatbotID;
            response = client.train(request);
            logger.info("train >> \n{}", response.toString());

            // 训练是一个长时任务，进行异步反馈


            while (true) {
                Thread.sleep(1000 * 3);
                response = client.status(request);
                if (response.rc == 0)
                    break;
            }

            /**
             * 与机器人对话
             */
            // 创建 session
            request = new Data();
            ChatSession session = new ChatSession();
            session.chatbotID = chatbotID;
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
            message.textMessage = "我想点外卖，来一份番茄";
            request.message = message;
            response = client.chat(request);
            logger.info("chat >> \n{}", response.toString());

            //Data(rc:0, session:ChatSession(intent_name:orderTakeOut, chatbotID:avtr002, uid:java, channel:testclient,
            // resolved:false, id:A3C4061B429C507A85CEF45D00000000, entities:[Entity(name:vegetable, val:番茄, requires:true, dictname:food), Entity(name:location, val:, requires:true, dictname:@LOC), Entity(name:date, val:, requires:true, dictname:@TIME)], branch:dev, createdate:2019-09-07 21:48:40, updatedate:2019-09-07 21:48:40),
            // message:ChatMessage(receiver:java, textMessage:
            //  外卖送到哪里, is_fallback:false, is_proactive:true))

            transport.close();
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        }
    }
}
