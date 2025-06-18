package com.inigolabs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.GroupLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("preview")
public class Foreign {
    private static final SymbolLookup LOOKUP;
    private static final Linker LINKER = Linker.nativeLinker();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final MethodHandle CreateFunc;
    private static final MethodHandle CheckLastErrorFunc;
    private static final MethodHandle ProcessRequestFunc;
    private static final MethodHandle ProcessResponseFunc;
    private static final MethodHandle DisposePinnerFunc; 
    private static final MethodHandle DisposeHandleFunc;
    private static final MethodHandle ShutdownFunc;
    private static final MethodHandle FlushFunc;
    private static final MethodHandle GetVersionFunc;
    private static final MethodHandle TestRuntimeFunc;
    private static final MethodHandle UpdateSchemaFunc;
    private static final MethodHandle CopyQuerydataFunc;
    private static final MethodHandle GatewayInfoFunc;
    private static final MethodHandle IsPersistingEnabledFunc;
    private static final MethodHandle CnoopFunc;

    public record ProcessRequestResult(
        long Handle,
        String Output,
        String Status,
        String Analysis,
        int StatusCode
    ) {}

    /*
        typedef struct {
            int8_t logLevel;
            char* name;
            char* service;
            char* token;
            char* schema;
            char* runtime;
            char* egressUrl;
            uintptr_t gateway;
            int8_t disableResponseData;
            int8_t federation;
        } Config;
     */
    private static final GroupLayout CONFIG_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_BYTE.withName("logLevel"),
        MemoryLayout.paddingLayout(7), // Padding for alignment
        ValueLayout.ADDRESS.withName("name"),
        ValueLayout.ADDRESS.withName("service"),
        ValueLayout.ADDRESS.withName("token"),
        ValueLayout.ADDRESS.withName("schema"),
        ValueLayout.ADDRESS.withName("runtime"),
        ValueLayout.ADDRESS.withName("egressUrl"),
        ValueLayout.JAVA_LONG.withName("gateway"),
        ValueLayout.JAVA_BYTE.withName("disableResponseData"),
        ValueLayout.JAVA_BYTE.withName("federation"),
        ValueLayout.JAVA_BYTE.withName("federationExample"),
        MemoryLayout.paddingLayout(5) // Padding for alignment
    );

    static {
        try {
            System.out.println("Loading Inigo Library...");
            
            String libPath = System.getenv("INIGO_LIBRARY_PATH");
            if (libPath != null && !libPath.isEmpty()) {
                System.out.println("Loading Inigo Foreign library from environment variable: " + libPath);
                System.load(libPath);
            } else if (Inigo.LibraryPath != null && !Inigo.LibraryPath.isEmpty()) {
                System.load(Inigo.LibraryPath);
            } else {
                System.setProperty("java.library.path", System.getProperty("user.dir"));
                System.loadLibrary("libinigo");
            }

            LOOKUP = SymbolLookup.loaderLookup();

            // create
            CreateFunc = mHandle("create", 
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

            // check_lasterror
            CheckLastErrorFunc = mHandle("check_lasterror", 
                FunctionDescriptor.of(ValueLayout.ADDRESS));

            // process_service_request_v2
            ProcessRequestFunc = mHandle("process_service_request_v2",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,    // return uintptr
                    ValueLayout.JAVA_LONG,    // handlePtr
                    ValueLayout.ADDRESS,      // subgraph_name
                    ValueLayout.JAVA_LONG,    // subgraph_name_len
                    ValueLayout.ADDRESS,      // header
                    ValueLayout.JAVA_LONG,    // header_len
                    ValueLayout.ADDRESS,      // input
                    ValueLayout.JAVA_LONG,    // input_len
                    ValueLayout.ADDRESS,      // output
                    ValueLayout.ADDRESS,      // output_len
                    ValueLayout.ADDRESS,      // status_output
                    ValueLayout.ADDRESS,      // status_output_len
                    ValueLayout.ADDRESS,      // analysis
                    ValueLayout.ADDRESS       // analysis_len
                ));

            // process_response
            ProcessResponseFunc = mHandle("process_response",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,    // return uintptr
                    ValueLayout.JAVA_LONG,    // handlePtr
                    ValueLayout.JAVA_LONG,    // reqHandle
                    ValueLayout.ADDRESS,      // input
                    ValueLayout.JAVA_LONG,    // input_len
                    ValueLayout.ADDRESS,      // output
                    ValueLayout.ADDRESS       // output_len
                ));

            // dispose_pinner
            DisposePinnerFunc = mHandle("disposePinner", // TODO: dispose_pinner
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)); // id uintptr

            // dispose_handle
            DisposeHandleFunc = mHandle("disposeHandle", // TODO: dispose_handle
                FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)); // id uintptr

            // shutdown
            ShutdownFunc = mHandle("shutdown", 
                FunctionDescriptor.ofVoid());

            // flush
            FlushFunc = mHandle("flush", 
                FunctionDescriptor.ofVoid());

            // get_version
            GetVersionFunc = mHandle("get_version", 
                FunctionDescriptor.of(ValueLayout.ADDRESS));

            // test_runtime
            TestRuntimeFunc = mHandle("test_runtime",
                FunctionDescriptor.ofVoid());

            // update_schema
            UpdateSchemaFunc = mHandle("update_schema",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_BOOLEAN, // return boolean
                    ValueLayout.JAVA_LONG,    // handlePtr
                    ValueLayout.ADDRESS,      // schema
                    ValueLayout.JAVA_LONG     // schema_len
                ));

            // copy_querydata
            CopyQuerydataFunc = mHandle("copy_querydata",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,   // return uintptr
                    ValueLayout.JAVA_LONG    // handlePtr
                ));

            // gateway_info
            GatewayInfoFunc = mHandle("gateway_info",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_LONG,    // return uintptr
                    ValueLayout.JAVA_LONG,    // handlePtr
                    ValueLayout.ADDRESS,      // output
                    ValueLayout.ADDRESS       // output_len
                ));

            // is_persisting_enabled
            IsPersistingEnabledFunc = mHandle("is_persisting_enabled",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_BOOLEAN, // return bool
                    ValueLayout.JAVA_LONG     // handlePtr
                ));

            // cnoop
            CnoopFunc = mHandle("cnoop",
                FunctionDescriptor.ofVoid());

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Foreign library", e);
        }
    }

    private static MethodHandle mHandle(String symbolName, FunctionDescriptor descriptor) {
        var handle = LOOKUP.find(symbolName).map(symbol -> LINKER.downcallHandle(symbol, descriptor));
        return handle.orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolName));
    }

    /**
     * Creates an Inigo instance with the provided configuration.
     *
     * @param config The configuration for the Inigo instance.
     * @return The handle to the created Inigo instance.
     */
    public static long Create(Inigo.Config config) {
        try (var arena = Arena.ofConfined()) {
            var configSegment = arena.allocate(CONFIG_LAYOUT);
            
            setField(configSegment, "logLevel", (byte) config.LogLevel);
            setField(arena, configSegment, "name", config.Name);
            setField(arena, configSegment, "service", config.ServiceURL);
            setField(arena, configSegment, "token", config.Token);
            setField(arena, configSegment, "schema", config.Schema);
            setField(arena, configSegment, "runtime", config.Runtime);
            setField(arena, configSegment, "egressUrl", config.EgressURL);
            setField(configSegment, "gateway", config.Gateway);
            setField(configSegment, "disableResponseData", (byte) (config.DisableResponseData ? 1 : 0));
            setField(configSegment, "federation", (byte) (config.Federation ? 1 : 0));
            setField(configSegment, "federationExample", (byte) (config.FederationExample ? 1 : 0));

            return (long) CreateFunc.invokeExact(configSegment);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call create function", e);
        }
    }

    /**
     * Checks the last error and returns the error message if any.
     *
     * @return The last error message, or null if there is no error.
     */
    public static String CheckLastError() {
        try {
            var resultSegment = (MemorySegment) CheckLastErrorFunc.invokeExact();
            if (resultSegment.address() == 0) {
                return null;
            }
            var cString = resultSegment.reinterpret(Long.MAX_VALUE);
            return getString(cString);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call check_lasterror function", e);
        }
    }

    /**
     * Processes a request using the Inigo service.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @param subgraphName The name of the subgraph to process.
     * @param header The headers for the request.
     * @param input The input data for the request.
     * @return A ProcessRequestResult containing the output, status, and analysis.
     */
    public static ProcessRequestResult ProcessRequest(long instanceHandle, String subgraphName, Object header, byte[] input) {
        byte[] headers = null;
        if (header != null) {
            try {
                headers = objectMapper.writeValueAsBytes(header);
            } catch (Exception e) {
                System.err.println("ERROR: Failed to serialize headers: " + e.getMessage());
            }
        }
        return ProcessRequest(instanceHandle, subgraphName, headers, input);
    }

    /**
     * Processes a request using the Inigo service.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @param subgraphName The name of the subgraph to process.
     * @param header The headers for the request.
     * @param input The input data for the request.
     * @return A ProcessRequestResult containing the output, status, and analysis.
     */
    public static ProcessRequestResult ProcessRequest(long instanceHandle, String subgraphName, byte[] header, byte[] input) {
        try (var arena = Arena.ofConfined()) {
            long headerLength = header != null ? header.length : 0;
            var headerSegment = (header != null && header.length > 0) ? 
                allocateFromNullTerminated(arena, header) : MemorySegment.NULL;

            long subgraphNameLength = subgraphName != null ? subgraphName.length() : 0;
            var subgraphNameSegment = (subgraphName != null && !subgraphName.isEmpty()) ? 
                allocateFromNullTerminated(arena, subgraphName) : MemorySegment.NULL;

            long inputLength = input != null ? input.length : 0;
            var inputSegment = (input != null && input.length > 0) ? 
                allocateFromNullTerminated(arena, input) : MemorySegment.NULL;

            var outputPtr = arena.allocate(ValueLayout.ADDRESS);
            var outputLenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            var statusOutputPtr = arena.allocate(ValueLayout.ADDRESS);
            var statusOutputLenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            var analysisPtr = arena.allocate(ValueLayout.ADDRESS);
            var analysisLenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            
            var requestHandle = (long) ProcessRequestFunc.invokeExact(
                instanceHandle,
                subgraphNameSegment,
                subgraphNameLength,
                headerSegment,
                headerLength,
                inputSegment,
                inputLength,
                outputPtr,
                outputLenPtr,
                statusOutputPtr,
                statusOutputLenPtr,
                analysisPtr,
                analysisLenPtr
            );

            int statusCode = 200;
            long code = statusOutputLenPtr.get(ValueLayout.JAVA_LONG, 0);
            if (code < 0) {
                statusCode = (int)-code;
            }

            if (outputLenPtr.get(ValueLayout.JAVA_LONG, 0) > 0) {
                var response = extractString(outputPtr, outputLenPtr);
                Foreign.DisposeHandle(requestHandle);    
                return new ProcessRequestResult(0, response, null, null, statusCode);
            }

            var status = extractString(statusOutputPtr, statusOutputLenPtr);
            var analysis = extractString(analysisPtr, analysisLenPtr);

            Foreign.DisposePinner(requestHandle);
            return new ProcessRequestResult(requestHandle, null, status, analysis, statusCode);

        } catch (Throwable e) {
            throw new RuntimeException("Failed to call process_service_request_v2 function", e);
        }
    }

    /**
     * Processes a response using the Inigo service.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @param requestHandle The handle to the request.
     * @param input The input data for the response.
     * @return The processed response as a string.
     */
    public static String ProcessResponse(long instanceHandle, long requestHandle, byte[] input) {
        try (var arena = Arena.ofConfined()) {
            long inputLength = input != null ? input.length : 0;
            var inputSegment = (input != null && input.length > 0) ? 
                allocateFromNullTerminated(arena, input) : MemorySegment.NULL;
            
            var outputPtr = arena.allocate(ValueLayout.ADDRESS);
            var outputLenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            
            var handle = (long) ProcessResponseFunc.invokeExact(
                instanceHandle,
                requestHandle,
                inputSegment,
                inputLength,
                outputPtr,
                outputLenPtr
            );

            String output = null;
            if (outputLenPtr.get(ValueLayout.JAVA_LONG, 0) > 0) {
                output = extractString(outputPtr, outputLenPtr);
            }

            Foreign.DisposeHandle(handle);
            return output;

        } catch (Throwable e) {
            throw new RuntimeException("Failed to call process_response function", e);
        }
    }

    /**
     * Disposes the pinner associated with the given ID.
     *
     * @param id The ID of the pinner to dispose.
     */
    public static void DisposePinner(long id) {
        try {
            DisposePinnerFunc.invokeExact(id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call disposePinner function", e);
        }
    }

    /**
     * Disposes the handle associated with the given ID.
     *
     * @param id The ID of the handle to dispose.
     */
    public static void DisposeHandle(long id) {
        try {
            DisposeHandleFunc.invokeExact(id);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call disposeHandle function", e);
        }
    }

    /**
     * Shuts down the Inigo service.
     */
    public static void Shutdown() {
        try {
            ShutdownFunc.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call shutdown function", e);
        }
    }

    /**
     * Flushes the Inigo service.
     */
    public static void Flush() {
        try {
            FlushFunc.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call flush function", e);
        }
    }

    /**
     * Retrieves the version of the Inigo service.
     *
     * @return The version string, or null if not available.
     */
    public static String GetVersion() {
        try {
            var versionSegment = (MemorySegment) GetVersionFunc.invokeExact();
            if (versionSegment.address() == 0) {
                return null;
            }
            var cString = versionSegment.reinterpret(Long.MAX_VALUE);
            return getString(cString);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call get_version function", e);
        }
    }

    /**
     * Tests the runtime functionality of the Inigo service.
     * This is a no-op function that can be used to verify that the runtime is functioning correctly.
     */
    public static void TestRuntime() {
        try {
            TestRuntimeFunc.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call test_runtime function", e);
        }
    }

    /**
     * Updates the schema of the Inigo service.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @param schema The new schema to set.
     * @return true if the schema was updated successfully, false otherwise.
     */
    public static boolean UpdateSchema(long instanceHandle, String schema) {
        if (schema == null || schema.isEmpty()) {
            throw new IllegalArgumentException("Schema cannot be null or empty");
        }
        var schemaBase64 = Base64.getEncoder().encodeToString(schema.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        try (var arena = Arena.ofConfined()) {
            var schemaSegment = allocateFromNullTerminated(arena, schemaBase64);
            var schemaLen = (long) schema.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            return (boolean) UpdateSchemaFunc.invokeExact(instanceHandle, schemaSegment, schemaLen);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call update_schema function", e);
        }
    }

    /**
     * Copies query data from the Inigo service.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @return The copied query data as a long value.
     */
    public static long CopyQuerydata(long instanceHandle) {
        try {
            return (long) CopyQuerydataFunc.invokeExact(instanceHandle);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call copy_querydata function", e);
        }
    }

    /**
     * Retrieves gateway information from the Inigo service.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @return The gateway information as a string, or null if not available.
     */
    public static String GatewayInfo(long instanceHandle) {
        try (var arena = Arena.ofConfined()) {
            var outputPtr = arena.allocate(ValueLayout.ADDRESS);
            var outputLenPtr = arena.allocate(ValueLayout.JAVA_LONG);
            
            var handle = (long) GatewayInfoFunc.invokeExact(
                instanceHandle,
                outputPtr,
                outputLenPtr
            );
            
            var output = extractString(outputPtr, outputLenPtr);
            Foreign.DisposeHandle(handle);
            return output;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call gateway_info function", e);
        }
    }

    /**
     * Checks if persisting is enabled for the Inigo instance.
     *
     * @param instanceHandle The handle to the Inigo instance.
     * @return true if persisting is enabled, false otherwise.
     */
    public static boolean IsPersistingEnabled(long instanceHandle) {
        try {
            return (boolean) IsPersistingEnabledFunc.invokeExact(instanceHandle);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call is_persisting_enabled function", e);
        }
    }

    /**
     * No-op function to test if the Inigo service is functioning correctly.
     * This function does not perform any operations and is used for testing purposes.
     */
    public static void NoOp() {
        try {
            CnoopFunc.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call cnoop function", e);
        }
    }

    // private static MemorySegment allocateFrom(Arena arena, String str) {
    //     var bytes = str.getBytes(StandardCharsets.UTF_8);
    //     var segment = arena.allocate(bytes.length);

    //     segment.asByteBuffer().put(bytes);
    //     return segment;
    // }

    private static MemorySegment allocateFromNullTerminated(Arena arena, String str) {
        var bytes = str.getBytes(StandardCharsets.UTF_8);
        var segment = arena.allocate(bytes.length + 1);

        // Add null-termination
        segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
        
        segment.asByteBuffer().put(bytes);
        return segment;
    }

    private static MemorySegment allocateFromNullTerminated(Arena arena, byte[] bytes) {
        // var bytes = str.getBytes(StandardCharsets.UTF_8);
        var segment = arena.allocate(bytes.length + 1);

        // Add null-termination
        segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
        segment.asByteBuffer().put(bytes);
        return segment;
    }

    private static String getString(MemorySegment cString) {
        var buffer = cString.asByteBuffer();
        var len = 0;

        // Find null-termination
        while (len < buffer.capacity() && buffer.get(len) != 0) {
            len++;
        }

        var bytes = new byte[len];
        buffer.get(bytes, 0, len);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String extractString(MemorySegment stringPtr, MemorySegment lengthPtr) {
        var stringSegment = stringPtr.get(ValueLayout.ADDRESS, 0);
        if (stringSegment.address() == 0) {
            return null;
        }
        var length = lengthPtr.get(ValueLayout.JAVA_LONG, 0);
        if (length == 0) {
            return null;
        }
        var boundedSegment = stringSegment.reinterpret(length);
        // var bytes = boundedSegment.asByteBuffer().array();
        // return new String(bytes, java.nio.charset.StandardCharsets.UTF_8); 
        return Charset.forName("UTF-8").decode(boundedSegment.asByteBuffer()).toString();
    }

    private static void setField(MemorySegment segment, String fieldName, byte value) {
        segment.set(ValueLayout.JAVA_BYTE, CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(fieldName)), value);
    }
    
    private static void setField(MemorySegment segment, String fieldName, long value) {
        segment.set(ValueLayout.JAVA_LONG, CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(fieldName)), value);
    }
    
    private static void setField(Arena arena, MemorySegment segment, String fieldName, String value) {
        var stringSegment = allocateFromNullTerminated(arena, value);
        segment.set(ValueLayout.ADDRESS, CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(fieldName)), stringSegment);
    }
}
