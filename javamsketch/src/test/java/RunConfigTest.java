import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class RunConfigTest {
    @Test
    public void testTinyConf() throws Exception {
        RunConfig r = RunConfig.fromJsonFile("src/test/resources/tiny_conf.json");
        assertEquals(true, r.get("flag"));
        List<Double> vals = r.get("vals");
        assertEquals(2, vals.size());
    }

    @Test
    public void testFromString() throws IOException {
        String jsonConf = "{\"val\":1.0}";
        RunConfig r = RunConfig.fromJsonString(jsonConf);
        assertEquals(1.0, r.get("val"), 0);
    }

}