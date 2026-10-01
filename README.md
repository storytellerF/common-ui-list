# common-ui-list

[![codecov](https://codecov.io/gh/storytellerF/common-ui-list/graph/badge.svg?token=5IK0PP6G9G)](https://codecov.io/gh/storytellerF/common-ui-list)


ViewHolder 不再保存或提供 item holder。`bindData` 使用传入的数据；事件回调只按签名传参。
数据从 adapter 查询：子 adapter 使用 `getItemHolder(bindingAdapterPosition)`；整个列表使用
`recyclerView.adapter?.getItemHolderAt(absoluteAdapterPosition)`，支持嵌套 ConcatAdapter。
无效位置、加载状态头尾和 Paging 占位返回 null。自定义数据 adapter 需实现 `ItemHolderProvider`。

`@BindClickEvent` / `@BindLongClickEvent` 按方法签名中的参数名和声明顺序传参：

| 参数名 | 传入值 |
| --- | --- |
| `bindingAdapterPosition` | 当前子 adapter 内的索引，类型为 `Int` |
| `absoluteAdapterPosition` | 整个 RecyclerView 中的索引，类型为 `Int` |
| `viewholder`（兼容 `viewHolder`） | 当前 ViewHolder |
| `view` | 点击的 View；Compose 中为 ViewHolder 的 itemView |
| `binding` | ViewBinding 对象；Compose 中为 EDComposeView |

参数可以任意排序或组合，也可省略。请求的索引无效时不调用回调。
接收者可通过对应 adapter 的 `getItemHolder(bindingAdapterPosition)` 查询数据。
未知参数名或非 `Int` 的索引参数会产生 KSP 编译错误。
