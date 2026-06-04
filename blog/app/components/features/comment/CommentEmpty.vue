<template>
  <div class="comments-empty">
    <i class="ri-chat-off-line" />
    <p>暂无评论，快来发表第一条评论吧！</p>
  </div>
</template>

<style lang="scss" scoped>
.comments-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--zblog-border-soft);
  border-radius: var(--zblog-radius-card);
  padding: 60px 20px;
  color: var(--zblog-text-muted);
  text-align: center;
  background: var(--zblog-surface-muted);

  i {
    font-size: 4rem;
    margin-bottom: 15px;
    opacity: 0.5;
  }

  p {
    font-size: 1rem;
    margin: 0;
  }
}

// 响应式设�?
@media screen and (max-width: 768px) {
  .comments-empty {
    padding: 40px 20px;

    i {
      font-size: 3rem;
    }
  }
}
</style>
