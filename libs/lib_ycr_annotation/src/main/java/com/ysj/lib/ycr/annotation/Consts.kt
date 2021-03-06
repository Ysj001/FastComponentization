package com.ysj.lib.ycr.annotation

/*
 * 存放组件化框架用的常量
 *
 * @author Ysj
 * Create time: 2020/8/7
 */

/** 路由文件生成的根目录 */
const val PACKAGE_NAME_ROUTE = "com.ysj.ycr.generated.routes"

/** 文件名分隔符 */
const val SEPARATOR = "$$"

/** 路由生成的文件前缀 */
const val PREFIX_ROUTE = "Route${SEPARATOR}Path${SEPARATOR}"

/** 参数注入生成的文件后缀 */
const val SUFFIX_ROUTE_PARAM = "${SEPARATOR}RouteParam"