<?php

namespace OpenAPI\Client;

use OpenAPI\Client\Model\EnumTest;
use PHPUnit\Framework\TestCase;

class EnumTestTest extends TestCase
{
    public function testPossibleValues()
    {
        $this->assertSame(EnumTest::ENUM_STRING_UPPER, "UPPER");
        $this->assertSame(EnumTest::ENUM_STRING_LOWER, "lower");
        $this->assertSame(EnumTest::ENUM_STRING_EMPTY, "");
        $this->assertSame(EnumTest::ENUM_STRING_REQUIRED_UPPER, "UPPER");
        $this->assertSame(EnumTest::ENUM_STRING_REQUIRED_LOWER, "lower");
        $this->assertSame(EnumTest::ENUM_STRING_REQUIRED_EMPTY, "");
        $this->assertSame(EnumTest::ENUM_INTEGER_NUMBER_1, 1);
        $this->assertSame(EnumTest::ENUM_INTEGER_MINUS_1, -1);
        $this->assertSame(EnumTest::ENUM_NUMBER_NUMBER_1_DOT_1, 1.1);
        $this->assertSame(EnumTest::ENUM_NUMBER_MINUS_1_DOT_2, -1.2);
    }

    public function testStrictValidation()
    {
        $enum = new EnumTest([
            'enum_string' => 0,
        ]);

        $this->assertFalse($enum->valid());

        $expected = [
            "invalid value '0' for 'enum_string', must be one of 'UPPER', 'lower', ''",
            "'enum_string_required' can't be null",
        ];
        $this->assertSame($expected, $enum->listInvalidProperties());
    }

    public function testThrowExceptionWhenInvalidAmbiguousValueHasPassed()
    {
        $this->expectException(\InvalidArgumentException::class);
        $enum = new EnumTest();
        $enum->setEnumString(0);
    }

    public function testNonRequiredPropertyIsOptional()
    {
        $enum = new EnumTest([
            'enum_string_required' => 'UPPER',
        ]);
        $this->assertSame([], $enum->listInvalidProperties());
        $this->assertTrue($enum->valid());
    }

    public function testRequiredProperty()
    {
        $enum = new EnumTest();
        $this->assertSame(["'enum_string_required' can't be null"], $enum->listInvalidProperties());
        $this->assertFalse($enum->valid());
    }
}
