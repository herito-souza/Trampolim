package com.trampolim.api.trust;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class CnpjTest {
 @Test void numericCheckDigitsAndFormatting(){assertTrue(Cnpj.valid("11.222.333/0001-81"));assertFalse(Cnpj.valid("11222333000182"));assertFalse(Cnpj.valid("00000000000000"));assertFalse(Cnpj.valid("11111111111111"));}
 @Test void alphanumericUsesOfficialAsciiMinus48Rule(){assertTrue(Cnpj.valid("00.000.000/E08G-12"));assertTrue(Cnpj.valid("00.000.000/e08g-12"));assertFalse(Cnpj.valid("00000000E08G13"));}
 @Test void malformedInputIsNotSilentlyAccepted(){assertFalse(Cnpj.valid(null));assertEquals("ß",Cnpj.normalize("ß"));assertFalse(Cnpj.valid("11222333000181<script>"));assertFalse(Cnpj.valid("1122233300018"));assertFalse(Cnpj.valid("112223330001AA"));}
}
