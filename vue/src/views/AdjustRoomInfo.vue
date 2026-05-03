<template>
  <div>
    <el-breadcrumb separator-icon="ArrowRight" style="margin: 16px">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>申请管理</el-breadcrumb-item>
      <el-breadcrumb-item>调宿申请</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card style="margin: 15px; min-height: calc(100vh - 111px)">
      <div>
        <!--    功能区-->
        <div style="margin: 10px 0">
          <!--    搜索区-->
          <div style="margin: 10px 0">
            <el-input v-model="search" clearable placeholder="请输入学号" prefix-icon="Search" style="width: 20%"/>
            <el-button icon="Search" style="margin-left: 5px" type="primary" @click="load"></el-button>
            <el-button icon="refresh-left" style="margin-left: 10px" type="default" @click="reset"></el-button>
          </div>
        </div>
        <!--    表格-->
        <el-table v-loading="loading" :data="tableData" border max-height="705" style="width: 100%">
          <el-table-column label="#" type="index"/>
          <el-table-column label="学号" prop="username" sortable width="100px"/>
          <el-table-column label="姓名" prop="name" width="100px"/>
          <el-table-column label="当前房间号" prop="currentRoomId" sortable/>
          <el-table-column label="当前床位号" prop="currentBedId" sortable/>
          <el-table-column label="目标房间号" prop="towardsRoomId" sortable/>
          <el-table-column label="目标床位号" prop="towardsBedId" sortable/>
          <el-table-column
              :filter-method="filterTag"
              :filters="[
              { text: '待审核', value: 'pending' },
              { text: '审核通过', value: 'approved' },
              { text: '审核不通过', value: 'rejected' },
              { text: '处理中', value: 'in_progress' },
              { text: '已完成', value: 'completed' },
              { text: '拒绝执行', value: 'execution_rejected' },
            ]"
              filter-placement="bottom-end"
              label="申请状态"
              prop="state"
              sortable
              width="130px"
          >
            <template #default="scope">
              <el-tag :type="getStateType(scope.row.state)"
                      disable-transitions
              >{{ stateMap(scope.row.state) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" prop="applyTime" sortable/>
          <el-table-column label="处理时间" prop="finishTime" sortable/>
          <!--      操作栏-->
          <el-table-column label="操作" width="330px">
            <template #default="scope">
              <el-button icon="more-filled" type="default" @click="showDetail(scope.row)"></el-button>
              <!-- 宿管：只能审核待审核状态的申请 -->
              <el-button v-if="judgeIdentityForTemplate() === 1 && (scope.row.state === 'pending' || scope.row.state === '未处理')" 
                         icon="Edit" type="warning" @click="handleReview(scope.row)">审核</el-button>
              <!-- 管理员：只能处理审核通过的申请 -->
              <el-button v-if="judgeIdentityForTemplate() === 2 && (scope.row.state === 'approved' || scope.row.state === '通过')" 
                         icon="Edit" type="primary" @click="handleEdit(scope.row)">执行调宿</el-button>
              <el-popconfirm v-if="judgeIdentityForTemplate() === 2 && (scope.row.state === 'approved' || scope.row.state === '通过')"
                             title="确认拒绝执行该调宿申请？" @confirm="handleRejectExecute(scope.row)">
                <template #reference>
                  <el-button icon="Close" type="danger">拒绝执行</el-button>
                </template>
              </el-popconfirm>
              <!-- 管理员：确认处理中申请已完成 -->
              <el-button v-if="judgeIdentityForTemplate() === 2 && (scope.row.state === 'in_progress' || scope.row.state === '处理中')" 
                         icon="Check" type="success" @click="handleComplete(scope.row)">完成</el-button>
              <!-- 删除：只能删除已完成或审核不通过的记录 -->
              <el-popconfirm v-if="scope.row.state === 'completed' || scope.row.state === 'rejected' || scope.row.state === 'execution_rejected' || scope.row.state === '已完成' || scope.row.state === '驳回' || scope.row.state === '拒绝执行'" 
                             title="确认删除？" @confirm="handleDelete(scope.row.id)">
                <template #reference>
                  <el-button icon="Delete" type="danger"></el-button>
                </template>
              </el-popconfirm>
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
        <div>
          <!--      弹窗-->
          <el-dialog v-model="dialogVisible" title="操作" width="30%" @close="cancel">
            <el-form ref="form" :model="form" :rules="rules" label-width="120px">
              <el-form-item label="学号" prop="username">
                <el-input v-model="form.username" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item disabled label="当前房间号" prop="currentRoomId">
                <el-input v-model="form.currentRoomId" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="当前床位号" prop="currentBedId">
                <el-input v-model="form.currentBedId" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="目标房间号" prop="towardsRoomId">
                <el-input v-model="form.towardsRoomId" style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="目标床位号" prop="towardsBedId">
                <el-input v-model="form.towardsBedId" style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="申请时间" prop="applyTime" style="margin-top: 27px">
                <el-date-picker
                    v-model="form.applyTime"
                    clearable
                    disabled
                    placeholder="选择时间"
                    style="width: 50%"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                ></el-date-picker>
              </el-form-item>
            </el-form>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="cancel">取 消</el-button>
                <el-button type="primary" @click="save">确 定</el-button>
              </span>
            </template>
          </el-dialog>
          <!-- 宿管审核弹窗 -->
          <el-dialog v-model="reviewDialog" title="审核调宿申请" width="30%">
            <el-form label-width="120px">
              <el-form-item label="学号：">
                <span>{{ reviewForm.username }}</span>
              </el-form-item>
              <el-form-item label="姓名：">
                <span>{{ reviewForm.name }}</span>
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
          <!--详情信息弹窗-->
          <el-dialog v-model="detailDialog" title="学生信息" width="30%" @close="cancel">
            <el-form ref="form" :model="form" label-width="220px">
              <el-form-item label="学号：" prop="username">
                <template #default="scope">
                  <span>{{ form.username }}</span>
                </template>
              </el-form-item>
              <el-form-item label="姓名：" prop="name">
                <template #default="scope">
                  <span>{{ form.name }}</span>
                </template>
              </el-form-item>
              <el-form-item label="当前房间号：" prop="currentRoomId">
                <template #default="scope">
                  <span>{{ form.currentRoomId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="当前床位号：" prop="currentBedId">
                <template #default="scope">
                  <span>{{ form.currentBedId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="目标房间号：" prop="towardsRoomId">
                <template #default="scope">
                  <span>{{ form.towardsRoomId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="目标床位号：" prop="towardsBedId">
                <template #default="scope">
                  <span>{{ form.towardsBedId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="申请时间：" prop="applyTime">
                <template #default="scope">
                  <span>{{ form.applyTime }}</span>
                </template>
              </el-form-item>
              <el-form-item label="申请状态：" prop="state">
                <template #default="scope">
                  <span>{{ form.state }}</span>
                </template>
              </el-form-item>
              <el-form-item label="处理时间：" prop="finishTime">
                <template #default="scope">
                  <span>{{ form.finishTime }}</span>
                </template>
              </el-form-item>
            </el-form>
          </el-dialog>
        </div>
      </div>
    </el-card>
  </div>
</template>
<script src="@/assets/js/AdjustRoomInfo.js"></script>
