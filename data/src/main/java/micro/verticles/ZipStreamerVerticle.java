package micro.verticles;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import micro.entity.ZipStreamer;
import micro.service.DataStreamerService;
import micro.utils.UrlBuilder;
import rx.Observable;
import rx.Subscription;

import java.util.HashMap;
import java.util.stream.IntStream;

public class ZipStreamerVerticle extends DataStreamerVerticle{

  private Observable<JsonObject> consumer;
  private Observable<Long> timer;
  private Subscription consumerSub;
  private Subscription timerSub;
  private ZipStreamer streamer;
  private HashMap<String, JsonArray> inputs;

  public ZipStreamerVerticle(DataStreamerService service, ZipStreamer streamer) {
    super(service, streamer.getId());
    this.streamer = streamer;
    this.inputs = new HashMap<>();
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    consumerSub.unsubscribe();
    timerSub.unsubscribe();
  }

  @Override
  protected void initEBPub() {
    eb.<String>consumer(UrlBuilder.createKillUrl(id), stringMessage -> {
      if(stringMessage.body().equalsIgnoreCase("kill"))
        try {
          this.stop();
        } catch (Exception e) {
          e.printStackTrace();
        }
    });

    consumer = eb.<JsonObject>consumer(this.streamer.getInputUrl()).bodyStream().toObservable();
    consumerSub = consumer.
        filter(json -> !inputs.containsKey(json.getString("id")))
        .subscribe(o ->
              inputs.put(
                  o.getString("id"),
                  o.getJsonArray("data")
              )
        );

    timer = vertx.periodicStream(streamer.getTimer()).toObservable();
    timerSub = timer.
        subscribe(aLong -> {
          JsonObject output = new JsonObject();
          if(inputs.values().size() == streamer.getOrder().size()){
            JsonArray concatArray = new JsonArray();
            for(String s : streamer.getOrder().getSet()){
              inputs.get(s).forEach(o -> {
                JsonArray array = (JsonArray) o;
                IntStream.range(0, array.size())
                    .forEach(i -> concatArray.add(array.getDouble(i)));
              });
            }
            output.put("data", new JsonArray().add(concatArray));
            eb.publish(streamer.getOutputUrl(), output);
          }
          inputs.clear();
        });

  }

}
