package com.flink.samples.window;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class TumblingWindow {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> localhost = env.socketTextStream("localhost", 9000);
        SingleOutputStreamOperator<String> maped = localhost.map(x -> String.valueOf(x)).returns(Types.STRING);
        AllWindowedStream<String, TimeWindow> stringTimeWindowAllWindowedStream = localhost.timeWindowAll(Time.seconds(3));
        SingleOutputStreamOperator<String> sum = stringTimeWindowAllWindowedStream.sum(0);
        sum.print();
        env.execute("CountWindowAll");

    }

//    所有组都是3秒执行一次。
    public static void  keyedTumblingWindow() throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> localhost = executionEnvironment.socketTextStream("localhost", 9000);
        //        输入1583118000 spark,3  第一位是时间戳，注意是格林威治时间
//        1583118002 hadoop,2
        SingleOutputStreamOperator<Tuple2<String, Integer>> maped = localhost.map((String x) -> {
            String[] split = x.split(",");
            return Tuple2.of(split[0], Integer.parseInt(split[1]));
        }).returns(Types.TUPLE(Types.STRING, Types.INT));

        KeyedStream<Tuple2<String, Integer>, Tuple> keyed = maped.keyBy(0);
        keyed.timeWindow(Time.seconds(3)).sum(1).print();

        executionEnvironment.execute("keyedTumblingWindow");
    }

    //    所有组都是3秒执行一次。
    public static void  eventTumblingWindow() throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        executionEnvironment.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        SingleOutputStreamOperator<String> localhost = executionEnvironment.socketTextStream("localhost", 9000).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) {
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0]);
            }
        });
//        输入 spark,3
//        hadoop,2
        SingleOutputStreamOperator<Tuple2<String, Integer>> maped = localhost.map((String x) -> {
            String[] split = x.split(",");
            return Tuple2.of(split[1], Integer.parseInt(split[2]));
        }).returns(Types.TUPLE(Types.STRING, Types.INT));

        KeyedStream<Tuple2<String, Integer>, Tuple> keyed = maped.keyBy(0);
        keyed.timeWindow(Time.seconds(3)).sum(1).print();

        executionEnvironment.execute("eventTumblingWindow");
    }
}
