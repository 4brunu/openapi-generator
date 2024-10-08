/**
 * Encoding / Decoding Test
 * A spec to ensure encoding and decoding of types (both primitive and compound) works as expected
 *
 * OpenAPI spec version: latest
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { HttpFile } from '../http/http';

export class ComplexObject {
    'requiredProperty': string;
    'requiredNullableProperty': string | null;
    'optionalProperty'?: string;
    'optionalNullableProperty'?: string | null;

    static readonly discriminator: string | undefined = undefined;

    static readonly mapping: {[index: string]: string} | undefined = undefined;

    static readonly attributeTypeMap: Array<{name: string, baseName: string, type: string, format: string}> = [
        {
            "name": "requiredProperty",
            "baseName": "required_property",
            "type": "string",
            "format": ""
        },
        {
            "name": "requiredNullableProperty",
            "baseName": "required_nullable_property",
            "type": "string",
            "format": ""
        },
        {
            "name": "optionalProperty",
            "baseName": "optional_property",
            "type": "string",
            "format": ""
        },
        {
            "name": "optionalNullableProperty",
            "baseName": "optional_nullable_property",
            "type": "string",
            "format": ""
        }    ];

    static getAttributeTypeMap() {
        return ComplexObject.attributeTypeMap;
    }

    public constructor() {
    }
}
