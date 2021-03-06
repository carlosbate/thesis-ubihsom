package micro.verticles;

import io.vertx.core.json.JsonObject;
import micro.entity.MercuriusStreamer;
import micro.service.DataStreamerService;
import rx.Observable;
import rx.Subscription;

public class MercuriusStreamerVerticle extends DataStreamerVerticle {

    private Observable<JsonObject> consumer;
    private Subscription subscription;
    private MercuriusStreamer streamer;

    public MercuriusStreamerVerticle(DataStreamerService service, MercuriusStreamer streamer) {
        super(service, streamer.getId());
        this.streamer = streamer;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        subscription.unsubscribe();
    }

    @Override
    protected void initEBPub() {
        consumer = eb.<JsonObject>consumer(this.streamer.getInputUrl()).bodyStream().toObservable();
        subscription = consumer.subscribe(m ->  vertx.eventBus().publish(this.streamer.getOutputUrl(), m));
    }

}
