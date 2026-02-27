cmake_minimum_required(VERSION 3.22)
project(tthsd_godot CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# -------------------------------------------------------------------
# godot-cpp 路径（submodule 或手动指定）
# -------------------------------------------------------------------
set(GODOT_CPP_PATH "${CMAKE_CURRENT_SOURCE_DIR}/thirdparty/godot-cpp"
    CACHE PATH "godot-cpp 目录路径")

add_subdirectory(${GODOT_CPP_PATH} godot-cpp)

# -------------------------------------------------------------------
# TTHSD 动态库
# -------------------------------------------------------------------
# 通过 TTHSD_LIB_PATH 指定动态库路径或让链接器在运行时搜索
set(TTHSD_LIB_PATH "" CACHE PATH "TTHSD 动态库路径（留空则由运行时加载）")

# -------------------------------------------------------------------
# 插件共享库目标
# -------------------------------------------------------------------
file(GLOB_RECURSE SOURCES "src/*.cpp")

add_library(tthsd_godot SHARED ${SOURCES})

target_include_directories(tthsd_godot PRIVATE src)

target_link_libraries(tthsd_godot
    PRIVATE
    godot-cpp
)

# 若提供了 TTHSD 动态库路径，在编译时链接（便于符号检查）
if(TTHSD_LIB_PATH)
    target_link_libraries(tthsd_godot PRIVATE ${TTHSD_LIB_PATH})
endif()

# 产物放入 addons/tthsd/bin/
set_target_properties(tthsd_godot PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/project/addons/tthsd/bin"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/project/addons/tthsd/bin"
)
