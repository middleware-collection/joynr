/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2015 BMW Car IT GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
#include "joynr/ClassSerializer.h"
#include "joynr/SerializerRegistry.h"

namespace joynr
{

template <>
void ClassSerializer<int8_t>::serialize(const int8_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<uint8_t>::serialize(const uint8_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<int16_t>::serialize(const int16_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<uint16_t>::serialize(const uint16_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<int32_t>::serialize(const int32_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<uint32_t>::serialize(const uint32_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<int64_t>::serialize(const int64_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<uint64_t>::serialize(const uint64_t& i, std::ostream& stream)
{
    stream << std::to_string(i);
}

template <>
void ClassSerializer<double>::serialize(const double& i, std::ostream& stream)
{
    stream << i;
}

template <>
void ClassSerializer<float>::serialize(const float& i, std::ostream& stream)
{
    stream << i;
}

template <>
void ClassSerializer<bool>::serialize(const bool& i, std::ostream& stream)
{
    stream << i;
}

/**
 * @brief addEscapeForSpecialCharacters Escapes special characters in a string
 * @param str
 * @return
 */
static std::string addEscapeForSpecialCharacters(const std::string& str) {
    std::string escapedString;
    for(char c : str) {
        switch(c) {
        case '\b' :
            escapedString.push_back('\\');
            escapedString.push_back('b');
            break;
        case '\f' :
            escapedString.push_back('\\');
            escapedString.push_back('f');
            break;
        case '\n' :
            escapedString.push_back('\\');
            escapedString.push_back('n');
            break;
        case '\r' :
            escapedString.push_back('\\');
            escapedString.push_back('r');
            break;
        case '\t' :
            escapedString.push_back('\\');
            escapedString.push_back('t');
            break;
        case '\\' :
        case '\"' :
            escapedString.push_back('\\');
            escapedString.push_back(c);
            break;
        default:
            escapedString.push_back(c);
        }
    }

    return escapedString;
}

template <>
void ClassSerializer<std::string>::serialize(const std::string& s, std::ostream& stream)
{
    stream << '"' << addEscapeForSpecialCharacters(s) << '"';
}

template <>
void ClassSerializer<Variant>::serializeVariant(const Variant &variant, std::ostream &stream)
{
    serialize(variant, stream);
}

template <>
void ClassSerializer<Variant>::serialize(const Variant &variant,
                                             std::ostream &stream)
{
    std::string typeName = variant.getTypeName();
    auto serializer = SerializerRegistry::getSerializer(typeName);

    // Check that a Joynr serializer is available
    if (serializer) {
        serializer->serializeVariant(variant, stream);
    }
}

} /* namespace joynr */