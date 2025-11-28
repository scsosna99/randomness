package dev.scottsosna.sandbox.randomness.controller;

import dev.scottsosna.sandbox.randomness.supplier.ExternalSupplierRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.random.RandomGenerator;

@RestController
@RequestMapping("random")
public class RandomController {

    private RandomGenerator generator;

    @Autowired
    public RandomController(RandomGenerator generator) {
        this.generator = generator;
    }

    /**
     * getter
     * @return the next randomly-generated boolean
     */
    @GetMapping ("/boolean")
    public boolean getNextBoolean() {
        return generator.nextBoolean();
    }

    /**
     * getter
     * @return the next randomly-generated double
     */
    @GetMapping ("/double")
    public double getNextDouble() {
        return generator.nextDouble();
    }

    /**
     * getter
     * @return the next randomly-generated float
     */
    @GetMapping ("/float")
    public float getNextFloat() {
        return generator.nextFloat();
    }

    /**
     * getter
     * @return the next randomly-generated gaussian value
     */
    @GetMapping ("/gaussian")
    public double getGaussian() { return generator.nextGaussian(); }

    /**
     * getter
     * @return the next randomly-generated integer value
     */
    @GetMapping("/int")
    public int getNextInteger() {
        return generator.nextInt();
    }

    /**
     * getter
     * @param bound the upper-bound for the random number
     * @return the next randomly-generated integer
     */
    @GetMapping("/int/{bound}")
    public int getNextInteger (@PathVariable("bound") int bound) {
        return generator.nextInt (bound);
    }

    /**
     * getter
     * @return the next randomly-generated long value
     */
    @GetMapping("/long")
    public long getNextLong () {
        return generator.nextLong(); }

    /**
     * Initializes the specified supplier
     * @param supplierType supplier key as registered in the factory
     */
    @GetMapping("supplier/{supplierType}")
    public void getSupplier (@PathVariable("supplierType")  String supplierType) {
        if (generator instanceof ExternalSupplierRandom external) {
            external.initialize(supplierType);
        }
    }

    /**
     * Generates random data from specified supplier, downloaded as a file
     * @param supplierType supplier key as registered in the factory
     * @param longsCount the number of long numbers to generate
     * @return Streamable response entry which is downloaded as a file.
     */
    @GetMapping("supplier/{supplierType}/dump")
    public ResponseEntity<StreamingResponseBody> getStreamingResponse(
        @PathVariable("supplierType")  String supplierType,
        @RequestParam(value = "longsCount", required = false, defaultValue = "131072") int longsCount) {

        if (generator instanceof ExternalSupplierRandom external) {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=supplier-" + supplierType + ".bin");
            headers.add("Content-Transfer-Encoding", "binary");
            headers.add("Content-Length", String.valueOf(longsCount * Long.BYTES));

            return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body((os) -> external.dump(supplierType, os, longsCount));
        } else {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
