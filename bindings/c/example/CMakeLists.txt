cmake_minimum_required(VERSION 3.22)
project(tthsd_cpp_example CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# -------------------------------------------------------------------
# nlohmann/json（header-only，使用 FetchContent 自动下载）
# -------------------------------------------------------------------
include(FetchContent)
FetchContent_Declare(
    nlohmann_json
    URL https://github.com/nlohmann/json/releases/download/v3.11.3/json.tar.xz
)
FetchContent_MakeAvailable(nlohmann_json)

# -------------------------------------------------------------------
# 示例可执行文件
# -------------------------------------------------------------------
add_executable(download_example main.cpp)

target_include_directories(download_example PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/..   # 引用 tthsd.h 和 TTHSDownloader.hpp
)

target_link_libraries(download_example PRIVATE nlohmann_json::nlohmann_json)

# Linux: 需要 dl 库（dlopen/dlsym）
if(UNIX)
    target_link_libraries(download_example PRIVATE dl)
endif()
