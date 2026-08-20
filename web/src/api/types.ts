export type ApiError = {
  code: string;
  message: string;
  traceId: string;
  timestamp: string;
};

export type UserResponse = {
  id: number;
  username: string;
  phone: string;
  nickname: string;
  createdAt: string;
  updatedAt: string;
};

export type PointBalanceResponse = {
  userId: number;
  balance: number;
  accountCreated: boolean;
  updatedAt: string | null;
};

export type PointTransactionResponse = {
  transactionId: number;
  businessId: number;
  userId: number;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  source: string;
  remark: string | null;
  createdAt: string;
  replayed: boolean;
};

export type PointTransactionPageResponse = {
  items: PointTransactionResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type DailyCheckInResponse = {
  userId: number;
  businessDate: string;
  checkedInToday: boolean;
  currentStreak: number;
  rewardPoints: number;
  rewardStatus: 'AVAILABLE' | 'PENDING' | 'AWARDED';
  checkInId: number | null;
  pointTransactionId: number | null;
  balanceAfter: number | null;
  signedDates: string[];
};

export type ActivityType = 'CHECK_IN' | 'LOTTERY' | 'REDEMPTION';
export type ActivityStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ENDED';
export type PrizeType = 'VIRTUAL' | 'POINTS' | 'NONE';
export type PrizeStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE';

export type PrizeResponse = {
  id: number;
  code: string;
  name: string;
  type: PrizeType;
  status: PrizeStatus;
  availableStock: number;
  awardPayload: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreatePrizeRequest = {
  code: string;
  name: string;
  type: PrizeType;
  availableStock: number;
  awardPayload: string | null;
};

export type UpdatePrizeRequest = {
  name: string;
  type: PrizeType;
  status: PrizeStatus;
  awardPayload: string | null;
};

export type AdjustPrizeInventoryRequest = {
  businessNo: string;
  changeAmount: number;
  remark: string | null;
};

export type ActivitySummaryResponse = {
  id: number;
  code: string;
  type: ActivityType;
  name: string;
  startsAt: string;
  endsAt: string | null;
};

export type LotteryPrizeResponse = {
  id: number;
  prizeId: number;
  name: string;
  type: PrizeType;
  coverUrl: string | null;
  campaignQuota: number | null;
  displayOrder: number;
};

export type RedemptionItemResponse = {
  id: number;
  itemCode: string;
  prizeId: number;
  name: string;
  type: Exclude<PrizeType, 'NONE'>;
  coverUrl: string | null;
  pointsPrice: number;
  campaignQuota: number | null;
  displayOrder: number;
};

export type ActivityDetailResponse = ActivitySummaryResponse & {
  status: ActivityStatus;
  ruleVersion: number;
  pointsCost: number;
  dailyLimit: number | null;
  prizes: LotteryPrizeResponse[];
  items: RedemptionItemResponse[];
};

export type LoginResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  role: UserRole;
  permissions: PermissionCode[];
  user: UserResponse;
};

export type UserRole = 'USER' | 'ADMIN' | 'SUPER_ADMIN';
export type PermissionCode =
  | 'ACCOUNT_SELF'
  | 'POINTS_SELF'
  | 'CHECK_IN'
  | 'LOTTERY_PARTICIPATE'
  | 'REDEMPTION_PARTICIPATE'
  | 'ACTIVITY_MANAGE'
  | 'PRIZE_MANAGE'
  | 'INVENTORY_MANAGE'
  | 'ROLE_MANAGE';

export type AdminActivityResponse = ActivitySummaryResponse & {
  status: ActivityStatus;
  ruleVersion: number;
  pointsCost: number;
  dailyLimit: number | null;
  qualificationRule: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateActivityRequest = {
  code: string;
  name: string;
  type: Exclude<ActivityType, 'CHECK_IN'>;
  startsAt: string;
  endsAt: string | null;
  pointsCost: number;
  dailyLimit: number | null;
  qualificationRule: string | null;
};

export type UpdateActivityRequest = Omit<CreateActivityRequest, 'code' | 'type'> & {
  status: ActivityStatus;
};

export type LotteryDrawRequest = {
  requestId: string;
};

export type LotteryDrawResponse = {
  participationId: number;
  activityCode: string;
  userId: number;
  prizeId: number;
  prizeName: string;
  prizeType: PrizeType;
  coverUrl: string | null;
  won: boolean;
  pendingAwardCreated: boolean;
  pointsCost: number;
  pointTransactionId: number;
  balanceAfter: number;
  drawnAt: string;
};

export type LotteryRecordStatus = 'PROCESSING' | 'SUCCESS' | 'FAILED';

export type LotteryRecordResponse = {
  orderId: number;
  activityCode: string;
  activityName: string;
  status: LotteryRecordStatus;
  prizeId: number | null;
  prizeName: string | null;
  prizeType: PrizeType | null;
  pointsCost: number;
  createdAt: string;
  updatedAt: string;
};

export type RedemptionResponse = {
  redemptionId: number;
  activityCode: string;
  itemId: number;
  itemCode: string;
  userId: number;
  prizeId: number;
  prizeName: string;
  prizeType: Exclude<PrizeType, 'NONE'>;
  coverUrl: string | null;
  pointsCost: number;
  pointTransactionId: number;
  balanceAfter: number;
  pendingAwardCreated: boolean;
  redeemedAt: string;
};
