message(STATUS "#############################################")
message(STATUS "Configuring tests...")
message(STATUS "#############################################\n")

option(
    BUILD_UNIT_TESTS
    "Build unit tests?"
    ON
)

option(
    BUILD_INTEGRATION_TESTS
    "Build integration tests?"
    ON
)

option(
    BUILD_SYSTEM_INTEGRATION_TESTS
    "Build system integration tests?"
    ON
)

if(${WIN32})
    set(DISABLE_PTHREAD_CMAKE_ARG ON)
else()
endif(${WIN32})

include(AddGtestGmock)

### copy test resources and settings ##########################################
macro(CONFIGURE_TEST_FILES targetDirectory)
    set(TEST_FILES
        "AccessControlNoPermission.entries"
        "AccessControlYesPermission.entries"
        "AccessStoreTest.persist"
        "integrationtest.settings"
        "websocket-cc-tls.settings"
        "websocket-libjoynr-tls.settings"
        "websocket-libjoynr-non-tls.settings"
        "CCSettingsWithAccessControlDisabled.settings"
        "CCSettingsWithAccessControlEnabled.settings"
        "HttpSystemIntegrationTest1.settings"
        "HttpSystemIntegrationTest2.settings"
        "HttpJoynrClusterControllerRuntimeTest.settings"
        "MqttSystemIntegrationTest1.settings"
        "MqttSystemIntegrationTest2.settings"
        "MqttOverTLSSystemIntegrationTest1.settings"
        "MqttOverTLSSystemIntegrationTest2.settings"
        "MqttJoynrClusterControllerRuntimeTest.settings"
        "HttpMessagingSettingsTest.settings"
        "MqttMessagingSettingsTest.settings"
        "MessagingSettingsDiscoveryEntryExpiryIntervalMs.settings"
        "libjoynrSystemIntegration1.settings"
        "libjoynrSystemIntegration2.settings"
        "ca.crt"
        "testclient.pem"
    )

    foreach(testFile ${TEST_FILES})
        configure_file(
            "resources/${testFile}"
            "${targetDirectory}/test-resources/${testFile}"
            @ONLY
        )
    endforeach()
endmacro(CONFIGURE_TEST_FILES)

if(${CMAKE_GENERATOR} MATCHES "Visual Studio")
    CONFIGURE_TEST_FILES("${JOYNR_BINARY_DIR}/Debug")
    CONFIGURE_TEST_FILES("${JOYNR_BINARY_DIR}/Release")
else()
    CONFIGURE_TEST_FILES(${JOYNR_BINARY_DIR})
endif(${CMAKE_GENERATOR} MATCHES "Visual Studio")

file(COPY "resources/ListOfCapabilitiesToInject.json"
     DESTINATION "${JOYNR_BINARY_DIR}/test-resources/")

### set up stuff common for all test targets ##################################

if(${GENERATE_SOURCES})
    JoynrGenerator(
        OUTPUTPATH "${CMAKE_CURRENT_SOURCE_DIR}/gen"
        MODELPATH "${CMAKE_CURRENT_SOURCE_DIR}/../../basemodel/src/test/franca"
        GENERATION_LANGUAGE "cpp"
    )
endif(${GENERATE_SOURCES})

get_target_property(Joynr_PUBLIC_INCLUDE_DIRECTORY Joynr PUBLIC_INCLUDE_DIRECTORY)

get_target_property(joynrwsruntime_PUBLIC_INCLUDE_DIRECTORY JoynrWsRuntime PUBLIC_INCLUDE_DIRECTORY)
get_target_property(JoynrClusterController_PUBLIC_INCLUDE_DIRECTORY JoynrClusterController PUBLIC_INCLUDE_DIRECTORY)
get_target_property(JoynrClusterControllerRuntime_PUBLIC_INCLUDE_DIRECTORY JoynrClusterControllerRuntime PUBLIC_INCLUDE_DIRECTORY)

set(
    test_INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_SOURCE_DIR}
    "${CMAKE_CURRENT_SOURCE_DIR}/.."
    "${CMAKE_CURRENT_SOURCE_DIR}/gen/include"
    ${Joynr_PUBLIC_INCLUDE_DIRECTORY}
    ${joynrwsruntime_PUBLIC_INCLUDE_DIRECTORY}
    ${JoynrClusterController_PUBLIC_INCLUDE_DIRECTORY}
    ${JoynrClusterControllerRuntime_PUBLIC_INCLUDE_DIRECTORY}
)

set(
    JoynrTest_SYSTEM_INCLUDE_DIRECTORIES
    ${spdlog_INCLUDE_DIRS}
)

file(
    GLOB_RECURSE JoynrTest_GENSOURCES
    RELATIVE ${CMAKE_CURRENT_SOURCE_DIR}
    "gen/*.cpp"
)

file(
    GLOB_RECURSE JoynrTest_GENHEADERS
    RELATIVE ${CMAKE_CURRENT_SOURCE_DIR}
    "gen/*.h"
)

add_library(TestGenerated SHARED
    ${JoynrTest_GENSOURCES}
    ${JoynrTest_GENHEADERS}
)

# add needed dependencies by linking to target Joynr
target_link_libraries(TestGenerated Joynr)

if(NOT USE_PLATFORM_SPDLOG)
    add_dependencies(TestGenerated spdlog)
endif(NOT USE_PLATFORM_SPDLOG)

target_include_directories(
    TestGenerated
    PUBLIC ${test_INCLUDE_DIRECTORIES}
)

target_include_directories(
    TestGenerated
    SYSTEM PUBLIC
    ${JoynrTest_SYSTEM_INCLUDE_DIRECTORIES}
)

if(${USE_DBUS_COMMONAPI_COMMUNICATION})
    set(
        OPTIONAL_DBUS_LIBRARIES
        JoynrDbusRuntime
    )
    get_target_property(common_api_PUBLIC_INCLUDE_DIRECTORY JoynrCommonAPI PUBLIC_INCLUDE_DIRECTORY)
    list(
        APPEND test_INCLUDE_DIRECTORIES
        ${common_api_PUBLIC_INCLUDE_DIRECTORY}
    )
else()
    set(OPTIONAL_DBUS_LIBRARIES "")
endif(${USE_DBUS_COMMONAPI_COMMUNICATION})

## Collect link targets for tests
set(
    test_TARGET_LIBRARIES
    Joynr
    JoynrClusterController
    JoynrClusterControllerRuntime
)

# Visual Studio uses different directories for RELEASE and DEBUG
if(${CMAKE_GENERATOR} MATCHES "Visual Studio")
    set(
        GMOCK_LINK_LIBRARY
        optimized ${googlemock_binary_dir}/Release/${CMAKE_FIND_LIBRARY_PREFIXES}gmock${CMAKE_FIND_LIBRARY_SUFFIXES}
        debug ${googlemock_binary_dir}/Debug/${CMAKE_FIND_LIBRARY_PREFIXES}gmock${CMAKE_FIND_LIBRARY_SUFFIXES}
    )
    set(
        GTEST_LINK_LIBRARY
        optimized ${googletest_binary_dir}/Release/${CMAKE_FIND_LIBRARY_PREFIXES}gtest${CMAKE_FIND_LIBRARY_SUFFIXES}
        debug ${googletest_binary_dir}/Debug/${CMAKE_FIND_LIBRARY_PREFIXES}gtest${CMAKE_FIND_LIBRARY_SUFFIXES}
    )

    list(
        APPEND test_TARGET_LIBRARIES
        ${GTEST_LINK_LIBRARY}
        ${GMOCK_LINK_LIBRARY}
    )
else()
    list(
        APPEND test_TARGET_LIBRARIES
        ${GTEST_LIBRARIES}
        ${GMOCK_LIBRARIES}
    )
endif(${CMAKE_GENERATOR} MATCHES "Visual Studio")


# Add additional boost dependencies for tests
list(APPEND JOYNR_BOOST_REQUIRED_COMPONENTS regex)
find_package(Boost ${JOYNR_BOOST_REQUIRED_VERSION} REQUIRED
    COMPONENTS ${JOYNR_BOOST_REQUIRED_COMPONENTS}
)
include(CheckBoostImportTargets)

list(
    APPEND test_TARGET_LIBRARIES
    Boost::regex
    # Link the Dbus runtime last so that the cluster controller
    # runtime is the default runtime
    ${OPTIONAL_DBUS_LIBRARIES}
)

## Collect common header files for tests
set(
    test_HEADERS
    "JoynrTest.h"
    "PrettyPrint.h"
    "utils/MockCallback.h"
    "utils/MockObjects.h"
    "utils/LibJoynrMockObjects.h"
    "utils/MockLocalCapabilitiesDirectoryCallback.h"
    "utils/TimeUtils.h"
    "utils/TestRunnable.h"
    "utils/TestLibJoynrWebSocketRuntime.h"
)

## Collect common source files for tests
set(
    test_SOURCES
    "JoynrTest.cpp"
    "PrettyPrint.cpp"
    "utils/MockObjects.cpp"
    "utils/TestRunnable.cpp"
    "utils/MockLocalCapabilitiesDirectoryCallback.cpp"
    "TestMain.cpp"
)

###############
# g_UnitTests #
###############

if(${BUILD_UNIT_TESTS})
    file(
        GLOB g_UnitTests_SOURCES
        "unit-tests/*.cpp"
        "unit-tests/mqtt/*.cpp"
        "unit-tests/websocket/*.cpp"
        "unit-tests/jsonserializer/*.cpp"
        "unit-tests/serializer/*.cpp"
    )

    file(
        GLOB g_UnitTests_HEADERS
        "unit-tests/*.h"
    )

    if(${USE_DBUS_COMMONAPI_COMMUNICATION})
        list(
            APPEND g_UnitTests_SOURCES
            "unit-tests/dbus/DbusSettingsTest.cpp"
        )
    endif(${USE_DBUS_COMMONAPI_COMMUNICATION})

    AddTest(
        g_UnitTests
        ${test_HEADERS}
        ${test_SOURCES}
        ${g_UnitTests_HEADERS}
        ${g_UnitTests_SOURCES}
    )

    target_link_libraries(
        g_UnitTests
        TestGenerated
        ${test_TARGET_LIBRARIES}
    )

    target_include_directories(
        g_UnitTests
        PRIVATE ${test_INCLUDE_DIRECTORIES}
    )

    install(TARGETS g_UnitTests TestGenerated
        RUNTIME DESTINATION ${INSTALL_TEST_DIR}
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    )
endif(${BUILD_UNIT_TESTS})

######################
# g_IntegrationTests #
######################

if(${BUILD_INTEGRATION_TESTS})
    file(
       GLOB g_IntegrationTests_SOURCES
       "integration-tests/*.cpp"
    )

    if(${USE_DBUS_COMMONAPI_COMMUNICATION})
       list(
           APPEND g_IntegrationTests_SOURCES
           "integration-tests/dbus/LibJoynrRuntimeTest.cpp"
           "integration-tests/dbus/LibJoynrDbusCommunicationTests.cpp"
       )
    endif(${USE_DBUS_COMMONAPI_COMMUNICATION})

    AddTest(
       g_IntegrationTests
       ${test_HEADERS}
       ${test_SOURCES}
       ${g_IntegrationTests_SOURCES}
       # Make the LibJoynrWebSocketRuntime available in parallel to the cluster controller runtime
       "../runtimes/libjoynr-runtime/websocket/LibJoynrWebSocketRuntime.h"
       "../runtimes/libjoynr-runtime/websocket/LibJoynrWebSocketRuntime.cpp"
       "../runtimes/libjoynr-runtime/LibJoynrRuntime.cpp"
    )

    target_link_libraries(
        g_IntegrationTests
        TestGenerated
        ${test_TARGET_LIBRARIES}
    )

    target_include_directories(
        g_IntegrationTests
        PRIVATE ${test_INCLUDE_DIRECTORIES}
    )

    install(TARGETS g_IntegrationTests TestGenerated
        RUNTIME DESTINATION ${INSTALL_TEST_DIR}
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    )
endif(${BUILD_INTEGRATION_TESTS})

############################
# g_SystemIntegrationTests #
############################

if(${BUILD_SYSTEM_INTEGRATION_TESTS})
    file(
       GLOB g_SystemIntegrationTests_SOURCES
       "systemintegration-tests/*.cpp"
    )

    if(${USE_DBUS_COMMONAPI_COMMUNICATION})
       list(
           APPEND g_SystemIntegrationTests_SOURCES
           "systemintegration-tests/dbus/End2EndDbusTest.cpp"
       )
    endif(${USE_DBUS_COMMONAPI_COMMUNICATION})

    AddTest(
       g_SystemIntegrationTests
       ${test_HEADERS}
       ${test_SOURCES}
       ${g_SystemIntegrationTests_SOURCES}
       "../runtimes/libjoynr-runtime/websocket/LibJoynrWebSocketRuntime.h"
       "../runtimes/libjoynr-runtime/websocket/LibJoynrWebSocketRuntime.cpp"
       "../runtimes/libjoynr-runtime/LibJoynrRuntime.cpp"
    )

    target_link_libraries(
        g_SystemIntegrationTests
        TestGenerated
        ${test_TARGET_LIBRARIES}
    )

    set_target_properties(
       g_SystemIntegrationTests
       PROPERTIES
       COMPILE_FLAGS "-Wno-effc++ -Wno-unused-parameter"
    )

    target_include_directories(
        g_SystemIntegrationTests
        SYSTEM
        PRIVATE ${CURL_INCLUDE_DIRS}
    )

    target_include_directories(
        g_SystemIntegrationTests
        PRIVATE ${test_INCLUDE_DIRECTORIES}
        PRIVATE ${BOOST_INCLUDE_DIRS}
    )

    install(TARGETS g_SystemIntegrationTests TestGenerated
        RUNTIME DESTINATION ${INSTALL_TEST_DIR}
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    )
endif(${BUILD_SYSTEM_INTEGRATION_TESTS})

install(DIRECTORY resources
        DESTINATION ${INSTALL_TEST_DIR}
)
