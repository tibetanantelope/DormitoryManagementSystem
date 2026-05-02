<template>
  <div>
    <el-breadcrumb separator-icon="ArrowRight" style="margin: 16px">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>信息管理</el-breadcrumb-item>
      <el-breadcrumb-item>报修信息</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card style="margin: 15px; min-height: calc(100vh - 111px)">
      <div>
        <!--    功能区-->
        <div style="margin: 10px 0">
          <!--    搜索区-->
          <div style="margin: 10px 0">
            <el-input v-model="search" clearable placeholder="请输入标题" prefix-icon="Search" style="width: 20%"/>
            <el-button icon="Search" style="margin-left: 5px" type="primary" @click="load"></el-button>
            <el-button icon="refresh-left" style="margin-left: 10px" type="default" @click="reset"></el-button>
          </div>
        </div>
        <!--    表格-->
        <el-table v-loading="loading" :data="tableData" border max-height="705" style="width: 100%">
          <el-table-column label="#" type="index"/>
          <el-table-column :show-overflow-tooltip="true" label="标题" prop="title"/>
          <el-table-column label="宿舍号" prop="dormBuildId" sortable width="150px"/>
          <el-table-column label="房间号" prop="dormRoomId" sortable width="150px"/>
          <el-table-column label="申请人" prop="repairer" width="150px"/>
          <el-table-column
              filter-placement="bottom-end"
              label="订单状态"
              prop="state"
              sortable
              width="120px"
          >
            <template #default="scope">
              <el-tag
                  :type="getStateType(scope.row.state)"
                  disable-transitions
              >{{ stateMap(scope.row.state) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="订单创建时间" prop="orderBuildTime" sortable width="180px"/>
          <el-table-column label="订单完成时间" prop="orderFinishTime" sortable width="180px"/>
          <!--      操作栏-->
          <el-table-column label="操作" width="280px">
            <template #default="scope">
              <!-- 详情按钮 -->
              <el-button icon="more-filled" type="default" @click="showDetail(scope.row)"></el-button>

              <!-- 宿管审核按钮 -->
              <!-- 宿管审核按钮 -->
              <el-button
                  v-if="scope.row.state === 'pending' && identity === 'dormManager'
                  && scope.row.dormBuildId === dormBuildId "
                  icon="Finished"
                  type="warning"
                  @click="handleReview(scope.row)"
              >
                审核
              </el-button>

              <!-- 管理员处理按钮 -->
              <el-button
                  v-if="(scope.row.state === 'approved' || scope.row.state === 'in_progress') && identity === 'admin'"
                  icon="Edit"
                  type="primary"
                  @click="handleEdit(scope.row)"
              >处理
              </el-button>

              <!-- 管理员完成按钮 -->
              <el-button
                  v-if="scope.row.state === 'in_progress' && identity === 'admin'"
                  icon="Checked"
                  type="success"
                  @click="handleComplete(scope.row)"
              >完成
              </el-button>

              <!-- 删除按钮：仅审核未通过 或 已完成 才显示 -->
              <el-button
                  type="danger"
                  size="mini"
                  @click="handleDelete(scope.row.id)"
                  v-if="scope.row.state === 'completed' || scope.row.state === 'rejected'"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <!--分页-->
        <div style="margin: 10px 0">
          <el-pagination
              v-model:currentPage="currentPage"
              :page-size="pageSize"
              :page-sizes="[10, 20]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          >
          </el-pagination>
        </div>
        <!--      弹窗-->
        <div>
          <!-- 宿管审核弹窗 -->
          <el-dialog v-model="reviewDialog" title="审核报修订单" width="30%">
            <el-form ref="reviewForm" :model="reviewForm" label-width="100px">
              <el-form-item label="报修标题">
                {{ reviewForm.title }}
              </el-form-item>
            </el-form>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="closeReview">取 消</el-button>
                <el-button type="danger" @click="reviewRejected">审核不通过</el-button>
                <el-button type="success" @click="reviewApproved">审核通过</el-button>
              </span>
            </template>
          </el-dialog>

          <!--   内容详情弹窗-->
          <el-dialog v-model="detailDialog" title="详情" width="30%">
            <el-card>
              <div v-html="detail.content"></div>
            </el-card>
            <template #footer>
              <span class="dialog-footer">
                <el-button type="primary" @click="closeDetails">确 定</el-button>
              </span>
            </template>
          </el-dialog>
        </div>
      </div>
    </el-card>
  </div>
</template>
<script src="@/assets/js/RepairInfo.js"></script>
